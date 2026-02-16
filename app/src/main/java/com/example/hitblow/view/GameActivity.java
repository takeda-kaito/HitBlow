package com.example.hitblow.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hitblow.R;
import com.example.hitblow.model.GameManager;
import com.example.hitblow.presenter.GamePresenter;

/**
 * ゲームプレイ画面の制御を担当するView層のActivity。
 * ユーザーの操作（入力、ボタン押下）を検知し、Presenterへ処理を委譲します。
 * また、Presenterからの指示に基づいて画面の状態を更新します。
 */
public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    // --- UIコンポーネント ---
    private TextView numberInputDisplay;       // 入力された数字を表示するエリア
    private TextView turnCountText;            // 現在のターン数を表示するテキスト
    private TextView timerText;                // 経過時間を表示するテキスト
    private LinearLayout historyLayout;        // 判定履歴の行を追加するコンテナ
    private LinearLayout numberCardsContainer; // CPUの隠し数字カードを表示するコンテナ
    private ScrollView historyScrollView;      // 履歴エリアのスクロール管理
    private Button callButton;                 // 判定実行ボタン
    private Button deleteButton;               // 一文字削除ボタン
    private View spacerForDelete;              // 削除ボタン横の余白調整用View
    private TextView callResultOverlay;        // 画面中央に表示される判定結果エフェクト
    private LinearLayout gameOverButtonsContainer; // ゲーム終了時に表示される操作パネル
    private Button restartButton;              // リトライボタン
    private Button mainMenuButton;             // メニュー戻るボタン
    private Button homeButton;                 // プレイ中のホーム戻るボタン
    private LinearLayout inputKeypadContainer; // 数字キーパッド全体のコンテナ

    // MVPパターンの各要素
    private GameManager gameManager;     // データの保持とロジック (Model)
    private GamePresenter gamePresenter; // 表示の制御 (Presenter)
    private int gameModeDigits;          // 選択されたゲームモード（3〜5桁）

    // ナンバーキーパッド（0〜9）のボタンIDを管理
    private final int[] numberKeyIds = {
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4,
            R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // UIコンポーネントの紐付け（findViewById）
        initViews();

        // 前の画面（ModeSelectActivity）から渡された桁数を取得。デフォルトは3桁。
        gameModeDigits = getIntent().getIntExtra(ModeSelectActivity.EXTRA_DIGITS, 3);

        // シングルトンからModelを取得し、ゲームをセットアップ
        gameManager = GameManager.getInstance();
        if (savedInstanceState == null) {
            // Activityの再生成（画面回転など）ではない新規起動時のみセットアップ
            gameManager.setupGame(gameModeDigits);
        }

        // Viewの各要素をPresenterに渡し、MVPの橋渡しを構築
        gamePresenter = new GamePresenter(
                this, gameManager, historyLayout, historyScrollView,
                numberInputDisplay, turnCountText, timerText, callResultOverlay,
                gameOverButtonsContainer, numberCardsContainer, inputKeypadContainer,
                callButton, deleteButton, spacerForDelete, numberKeyIds
        );

        // 桁数に合わせた正解カードの動的生成と初期表示
        setupNumberCards(gameModeDigits);
        gamePresenter.updateInputDisplay();

        // 各ボタンにクリックリスナーを設定
        setEventListeners();
    }

    /**
     * XML上の各UIパーツをJavaオブジェクトとして初期化します。
     */
    private void initViews() {
        numberInputDisplay = findViewById(R.id.number_input_display);
        turnCountText = findViewById(R.id.turn_count_text);
        timerText = findViewById(R.id.timer_text);
        historyLayout = findViewById(R.id.history_layout);
        numberCardsContainer = findViewById(R.id.number_cards_container);
        historyScrollView = findViewById(R.id.history_scrollview);
        callButton = findViewById(R.id.key_call);
        deleteButton = findViewById(R.id.key_delete);
        spacerForDelete = findViewById(R.id.spacer_for_delete);
        callResultOverlay = findViewById(R.id.call_result_overlay);
        homeButton = findViewById(R.id.button_home);
        inputKeypadContainer = findViewById(R.id.input_keypad_container);
        gameOverButtonsContainer = findViewById(R.id.game_over_buttons_container);

        // ゲームオーバー時のボタンコンテナ内を取得
        if (gameOverButtonsContainer != null) {
            restartButton = gameOverButtonsContainer.findViewById(R.id.button_restart);
            mainMenuButton = gameOverButtonsContainer.findViewById(R.id.button_main_menu);
        }
    }

    /**
     * ボタンのクリックイベントを一括で登録します。
     */
    private void setEventListeners() {
        // 数字ボタン(0-9)へのリスナー設定
        for (int id : numberKeyIds) {
            View keyView = findViewById(id);
            if (keyView != null) keyView.setOnClickListener(this);
        }
        // 特殊ボタンへのリスナー設定（Presenterへ処理を委譲）
        callButton.setOnClickListener(v -> gamePresenter.handleCall());
        deleteButton.setOnClickListener(v -> gamePresenter.handleDeleteInput());

        // ゲーム終了後の操作
        if (restartButton != null) restartButton.setOnClickListener(v -> restartGame());
        if (mainMenuButton != null) mainMenuButton.setOnClickListener(v -> backToMainMenu());

        // プレイ中の離脱確認
        homeButton.setOnClickListener(v -> showHomeConfirmationDialog());
    }

    /**
     * Activityが破棄される際のクリーンアップ処理。
     * メモリリーク防止のため、タイマー等のリソースを解放します。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gamePresenter != null) gamePresenter.stopTimer();
    }

    /**
     * CPUの隠し数字を表示するカードを桁数に応じて動的に生成します。
     * デザインの一貫性を保つため、プログラム上でサイズやマージンを計算します。
     */
    private void setupNumberCards(int digits) {
        numberCardsContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        // カードのサイズをdp単位からピクセル単位へ変換（56dp x 84dp）
        int cardWidthPx = (int) (56 * density);
        int cardHeightPx = (int) (84 * density);
        int cardMarginPx = (int) (8 * density);

        for (int i = 0; i < digits; i++) {
            TextView card = new TextView(this);
            card.setText("?"); // 初期状態は伏せられた状態
            card.setTextSize(48);
            card.setTextColor(0xFFFFFFFF);
            card.setBackgroundColor(0xFF333333); // 未公開時のダークグレー
            card.setGravity(Gravity.CENTER);
            card.setElevation(4f); // 少し浮かせたような影をつける

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidthPx, cardHeightPx);
            params.setMargins(cardMarginPx, 0, cardMarginPx, 0);
            card.setLayoutParams(params);

            numberCardsContainer.addView(card);
        }
    }

    /**
     * 数字ボタン（0〜9）がクリックされた際のコールバック。
     */
    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        // Presenterに入力内容を通知
        gamePresenter.handleNumberInput(b.getText().toString());
    }

    /**
     * 同じ設定でゲームを最初からやり直します。
     */
    private void restartGame() {
        gameManager.setupGame(gameModeDigits);
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(ModeSelectActivity.EXTRA_DIGITS, gameModeDigits);
        finish(); // 現在の画面を閉じ、新しいGameActivityを起動してリフレッシュ
        startActivity(intent);
    }

    /**
     * メインメニューに戻ります。
     */
    private void backToMainMenu() {
        if (gamePresenter != null) gamePresenter.stopTimer();
        Intent intent = new Intent(this, MainActivity.class);
        // メインメニューより上のActivityスタックをクリアして戻る
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * ゲーム中の誤操作による離脱を防ぐための確認ダイアログを表示します。
     */
    private void showHomeConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ゲームを終了しますか？")
                .setMessage("メインメニューに戻ると、現在の進行状況や経過時間はリセットされます。")
                .setPositiveButton("キャンセル", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("戻る", (dialog, which) -> backToMainMenu())
                .show();
    }
}