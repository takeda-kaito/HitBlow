package com.example.hitblow.presenter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hitblow.model.GameManager;
import com.example.hitblow.view.GameActivity;

/**
 * ゲームのUI更新、タイマー管理、イベント処理の架け橋となるPresenterクラス。
 * View(GameActivity)からのイベントを受け取り、Model(GameManager)を操作して
 * その結果をViewへ反映させる役割（MVPパターン）を担います。
 */
public class GamePresenter {

    private final Context context;
    private final GameManager gameManager;

    // UIコンポーネントの参照
    private final LinearLayout historyLayout;
    private final ScrollView historyScrollView;
    private final TextView numberInputDisplay;
    private final TextView turnCountText;
    private final TextView timerText;
    private final TextView callResultOverlay;
    private final LinearLayout gameOverButtonsContainer;
    private final LinearLayout numberCardsContainer;
    private final LinearLayout inputKeypadContainer;
    private final Button callButton;
    private final Button deleteButton;
    private final View spacerForDelete;

    // 非同期処理用ハンドラー（タイマーやオーバーレイ非表示用）
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private long startTime = 0L;
    private final long MILLIS_IN_SECOND = 1000L;

    // カラー定数（テーマに合わせた色指定）
    private final int COLOR_PURPLE = 0xFF5E35B1;
    private final int COLOR_GRAY = 0xFFAAAAAA;

    private final int[] numberKeyIds;
    private StringBuilder currentGuess; // 現在入力中の数字を保持
    private boolean isGameOver = false;

    /**
     * コンストラクタ。必要なUIコンポーネントとロジッククラスを紐付けます。
     */
    public GamePresenter(
            Context context, GameManager gameManager,
            LinearLayout historyLayout, ScrollView historyScrollView,
            TextView numberInputDisplay, TextView turnCountText,
            TextView timerText, TextView callResultOverlay,
            LinearLayout gameOverButtonsContainer, LinearLayout numberCardsContainer,
            LinearLayout inputKeypadContainer, Button callButton,
            Button deleteButton, View spacerForDelete, int[] numberKeyIds) {

        this.context = context;
        this.gameManager = gameManager;
        this.historyLayout = historyLayout;
        this.historyScrollView = historyScrollView;
        this.numberInputDisplay = numberInputDisplay;
        this.turnCountText = turnCountText;
        this.timerText = timerText;
        this.callResultOverlay = callResultOverlay;
        this.gameOverButtonsContainer = gameOverButtonsContainer;
        this.numberCardsContainer = numberCardsContainer;
        this.inputKeypadContainer = inputKeypadContainer;
        this.callButton = callButton;
        this.deleteButton = deleteButton;
        this.spacerForDelete = spacerForDelete;
        this.numberKeyIds = numberKeyIds;
        this.currentGuess = new StringBuilder();

        // 初期表示設定
        turnCountText.setText("TURN: 0");
        timerText.setText("TIME: 00:00");
    }

    /**
     * タイマー更新用の定期実行タスク
     */
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerText();
            timerHandler.postDelayed(this, MILLIS_IN_SECOND);
        }
    };

    /**
     * 数字ボタンが押された際の処理。
     * 入力制限の管理とディスプレイ更新を行います。
     */
    public void handleNumberInput(String digit) {
        if (isGameOver) return;
        int maxDigits = gameManager.getNumberOfDigits();
        if (currentGuess.length() < maxDigits) {
            currentGuess.append(digit);
            updateInputDisplay();
        }
    }

    /**
     * 削除ボタンが押された際の処理。一文字削除します。
     */
    public void handleDeleteInput() {
        if (isGameOver) return;
        if (currentGuess.length() > 0) {
            currentGuess.setLength(currentGuess.length() - 1);
            updateInputDisplay();
        }
    }

    /**
     * CALLボタンが押された際の処理。
     * 入力された数字とCPUの正解を照合し、結果を画面に反映します。
     */
    public void handleCall() {
        if (isGameOver) return;
        String input = currentGuess.toString();
        int gameModeDigits = gameManager.getNumberOfDigits();

        // 未入力チェック
        if (input.length() != gameModeDigits) {
            Toast.makeText(context, "入力が完了していません。", Toast.LENGTH_SHORT).show();
            return;
        }

        // 初回コール時にゲームを初期化（CPUナンバー生成）
        if (!gameManager.isCpuNumberSet()) {
            gameManager.setupGame(gameModeDigits);
        }

        boolean isFirstCall = (gameManager.getCurrentTurn() == 0);

        // ロジック層(Model)に判定を依頼
        int[] result = gameManager.processCall(input);
        int eats = result[0];
        int bites = result[1];

        if (eats != -1) {
            // 初回コール時のみタイマーを開始
            if (isFirstCall) startTimer();

            GameManager.HistoryEntry lastEntry = gameManager.getLastHistoryEntry();
            if (lastEntry != null) {
                // 結果の演出表示と履歴リストへの追加
                showCallResultOverlay(eats, bites);
                addHistoryEntry(lastEntry.turn, lastEntry.guess, lastEntry.eats, lastEntry.bites);
            }

            // 全桁一致(EAT)した場合はゲーム終了
            if (eats == gameModeDigits) gameOver();

            turnCountText.setText("TURN: " + gameManager.getCurrentTurn());
            clearInput();
        } else {
            Toast.makeText(context, "無効な番号です（数字の重複など）。", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 入力バッファをクリアし、画面表示をリセットします。
     */
    public void clearInput() {
        currentGuess.setLength(0);
        updateInputDisplay();
    }

    /**
     * ゲームクリア時の処理。タイマーを停止し、正解を表示してUIを終了状態に変更します。
     */
    public void gameOver() {
        isGameOver = true;
        stopTimer();

        // 操作UIを非表示にする
        if (inputKeypadContainer != null) inputKeypadContainer.setVisibility(View.GONE);
        callButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        spacerForDelete.setVisibility(View.GONE);

        // コンティニュー/終了ボタンを表示
        gameOverButtonsContainer.setVisibility(View.VISIBLE);
        turnCountText.setText("TURN: " + gameManager.getCurrentTurn());
        updateTimerText();

        // 伏せられていたCPUの正解カードを公開する
        if (gameManager.isCpuNumberSet()) {
            String cpuNumber = gameManager.getCpuNumber();
            for (int i = 0; i < numberCardsContainer.getChildCount(); i++) {
                View child = numberCardsContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView card = (TextView) child;
                    if (i < cpuNumber.length()) {
                        card.setText(String.valueOf(cpuNumber.charAt(i)));
                    }
                    card.setBackgroundColor(0xFF4CAF50); // 正解カラー（緑）に変更
                    card.setTextColor(0xFFFFFFFF);
                }
            }
        }
    }

    /**
     * 入力中の数字とキーパッドの状態を更新します。
     * 使用済みの数字ボタンを無効化し、ユーザーの誤入力を防止します。
     */
    public void updateInputDisplay() {
        String display = currentGuess.toString();
        int gameModeDigits = gameManager.getNumberOfDigits();

        // 入力状況を「-」を使って視覚的に表示
        StringBuilder sb = new StringBuilder(display);
        while (sb.length() < gameModeDigits) sb.append("-");
        numberInputDisplay.setText(sb.toString());

        // ボタンの色の状態（Enabled/Disabled）を管理
        int[][] statesKeypad = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_enabled}
        };
        int[] colorsKeypad = new int[]{COLOR_GRAY, COLOR_PURPLE};
        ColorStateList cslKeypad = new ColorStateList(statesKeypad, colorsKeypad);

        // キーパッドの有効/無効を切り替えて重複入力を防ぐ
        for (int id : numberKeyIds) {
            Button keyButton = ((GameActivity) context).findViewById(id);
            String digit = keyButton.getText().toString();
            boolean isUsed = display.contains(digit);
            keyButton.setEnabled(!isUsed);
            keyButton.setBackgroundTintList(cslKeypad);
        }

        // 規定の桁数に達した時のみCALLボタンを表示
        if (currentGuess.length() == gameModeDigits) {
            callButton.setVisibility(View.VISIBLE);
            spacerForDelete.setVisibility(View.GONE);
        } else {
            callButton.setVisibility(View.GONE);
            spacerForDelete.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 判定履歴をスクロールビューに追加します。
     */
    private void addHistoryEntry(int turn, String guess, int eats, int bites) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        // 動的なテキスト生成
        row.addView(createHistoryTextView(guess, 2f, Gravity.CENTER));
        row.addView(createVerticalDivider());
        row.addView(createHistoryTextView(String.valueOf(eats), 1f, Gravity.CENTER));
        row.addView(createVerticalDivider());
        row.addView(createHistoryTextView(String.valueOf(bites), 1f, Gravity.CENTER));

        historyLayout.addView(row);
        // 新しい履歴が追加されたら一番下まで自動スクロール
        historyScrollView.post(() -> historyScrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * 履歴テーブルの見栄えを整えるための区切り線を生成します。
     */
    private View createVerticalDivider() {
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
        divider.setBackgroundColor(0xFFCCCCCC);
        return divider;
    }

    /**
     * 履歴行内の各テキスト要素を生成します。
     */
    private TextView createHistoryTextView(String text, float weight, int gravity) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(0xFF000000);
        textView.setGravity(gravity | Gravity.CENTER_VERTICAL);
        textView.setPadding(8, 0, 8, 0);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        return textView;
    }

    /**
     * 判定結果(EAT/BITE)を画面中央に強調表示（オーバーレイ）します。
     */
    public void showCallResultOverlay(int eats, int bites) {
        handler.removeCallbacksAndMessages(null);
        String eatsStr = String.valueOf(eats);
        String bitesStr = String.valueOf(bites);
        String fullText = eatsStr + "EAT " + bitesStr + "BITE";

        // SpannableStringを使用して、数字の部分だけサイズを大きくする演出
        SpannableString spannableString = new SpannableString(fullText);
        spannableString.setSpan(new RelativeSizeSpan(1.5f), 0, eatsStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int biteStart = eatsStr.length() + "EAT ".length();
        spannableString.setSpan(new RelativeSizeSpan(1.5f), biteStart, biteStart + bitesStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        callResultOverlay.setText(spannableString);
        callResultOverlay.setVisibility(View.VISIBLE);
        callResultOverlay.bringToFront();

        // 2秒後に自動的に非表示にする
        handler.postDelayed(() -> callResultOverlay.setVisibility(View.GONE), 2000);
    }

    /**
     * タイマーを開始します。
     */
    public void startTimer() {
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    /**
     * タイマーを停止します。
     */
    public void stopTimer() {
        if (timerHandler != null) timerHandler.removeCallbacks(timerRunnable);
    }

    /**
     * 経過時間を計算し、TIME: mm:ss 形式で表示を更新します。
     */
    public void updateTimerText() {
        long millis = startTime == 0L ? 0L : System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / MILLIS_IN_SECOND);
        timerText.setText(String.format("TIME: %02d:%02d", seconds / 60, seconds % 60));
    }
}