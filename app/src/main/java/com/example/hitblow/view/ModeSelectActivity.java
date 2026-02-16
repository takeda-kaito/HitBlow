package com.example.hitblow.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hitblow.R;

/**
 * ゲームの難易度（桁数）を選択する画面を担当するActivity。
 * 3桁、4桁、5桁のいずれかを選択させ、その情報を GameActivity へ橋渡しします。
 * View層として、設定情報の管理と画面遷移を制御します。
 */
public class ModeSelectActivity extends AppCompatActivity {

    /**
     * IntentでGameActivityに桁数データを渡す際のキー。
     * 以前の numer0n から現在のプロジェクト名である hitblow 体系に修正済みです。
     * 他のアプリとの衝突を避けるため、パッケージ名を含めた完全修飾名で定義しています。
     */
    public static final String EXTRA_DIGITS = "com.example.hitblow.DIGITS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // モード選択専用のレイアウトファイルを適用
        setContentView(R.layout.activity_mode_select);

        // --- UIコンポーネントの初期化 ---
        Button button3 = findViewById(R.id.button_mode_3); // 3桁モード：初級
        Button button4 = findViewById(R.id.button_mode_4); // 4桁モード：中級
        Button button5 = findViewById(R.id.button_mode_5); // 5桁モード：上級

        // --- クリックイベントの設定 ---
        // 各ボタンに対して、対応する桁数を引数として startGame メソッドを呼び出します。
        button3.setOnClickListener(v -> startGame(3));
        button4.setOnClickListener(v -> startGame(4));
        button5.setOnClickListener(v -> startGame(5));
    }

    /**
     * 選択された桁数情報を Intent に詰め込み、GameActivity を起動します。
     *
     * @param digits プレイヤーが選択した桁数 (3, 4, 5)
     */
    private void startGame(int digits) {
        // 同じ view パッケージ内に配置された GameActivity への遷移準備
        Intent intent = new Intent(ModeSelectActivity.this, GameActivity.class);

        // Intentの付随情報(Extra)として桁数をセット
        // これにより GameActivity 側で「今何桁モードで遊んでいるか」が判定可能になります。
        intent.putExtra(EXTRA_DIGITS, digits);

        // 次の画面へ遷移
        startActivity(intent);
    }
}