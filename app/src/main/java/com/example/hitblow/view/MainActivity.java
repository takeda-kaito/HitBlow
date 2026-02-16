package com.example.hitblow.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hitblow.R;

/**
 * アプリ起動時に最初に表示されるメインメニュー画面。
 * プレイヤーに対してゲームモード（CPU対戦、または将来的な対人戦）の選択肢を提供します。
 * View層として、画面遷移のハンドリングに特化しています。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // メインメニューのレイアウトファイル (activity_main.xml) を適用
        setContentView(R.layout.activity_main);

        // --- UIコンポーネントの初期化 ---

        // 「一人で遊ぶ」ボタン：CPUと対戦する標準モードへの入り口
        Button onePlayerButton = findViewById(R.id.one_player_mode_button);

        // 「誰かと対戦」ボタン：将来的なマルチプレイ実装を見越したプレースホルダー
        Button twoPlayerButton = findViewById(R.id.two_player_mode_button);

        // --- クリックイベントの設定 ---

        /**
         * 「一人で遊ぶ」ボタン押下時：
         * 桁数（難易度）を選択する ModeSelectActivity へ遷移します。
         */
        onePlayerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ModeSelectActivity.class);
            startActivity(intent);
        });

        /**
         * 「誰かと対戦」ボタン押下時：
         * 現在は開発中のため、ユーザーに状況を伝えるフィードバックを表示します。
         */
        twoPlayerButton.setOnClickListener(v -> {
            // Toastを使用して、簡潔に未実装である旨を通知
            Toast.makeText(MainActivity.this, "未実装です", Toast.LENGTH_SHORT).show();
        });
    }
}