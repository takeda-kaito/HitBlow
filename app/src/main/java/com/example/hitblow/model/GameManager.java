package com.example.hitblow.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ゲームのコアロジック（CPUナンバー生成、ヒット＆ブロー判定、履歴管理）を担うシングルトンクラス。
 * Model層として、データの保持と計算に特化しています。
 * 以前の名称（numer0n）から hitblow 体系に統合されました。
 */
public class GameManager {
    private static GameManager instance;
    private String cpuNumber;      // CPUが生成した正解の数字列
    private int numberOfDigits;    // ゲームで設定された桁数（3桁〜5桁など）
    private int currentTurn;       // 現在のターン数

    /**
     * 1回ごとのコール（回答）結果を保持するデータクラス。
     * 履歴表示用のリストで使用します。
     */
    public static class HistoryEntry {
        public final int turn;      // ターン番号
        public final String guess;  // プレイヤーが入力した数字
        public final int eats;       // ヒット（位置も数字も一致）の数
        public final int bites;      // ブロー（数字は合うが位置が違う）の数

        public HistoryEntry(int turn, String guess, int eats, int bites) {
            this.turn = turn;
            this.guess = guess;
            this.eats = eats;
            this.bites = bites;
        }
    }

    // 過去の回答履歴を保持するリスト
    private List<HistoryEntry> history;

    /**
     * コンストラクタ。
     * シングルトンパターンのため外部からのインスタンス化を禁止しています。
     */
    private GameManager() {
        history = new ArrayList<>();
    }

    /**
     * GameManagerの唯一のインスタンスを取得します。
     * マルチスレッド環境を考慮し、synchronizedキーワードで安全性を確保しています。
     *
     * @return GameManagerのインスタンス
     */
    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    /**
     * ゲームの初期セットアップを行います。
     * 桁数の設定、正解番号の生成、履歴のリセットを同時に実行します。
     *
     * @param digits プレイヤーが選択した桁数（3, 4, 5など）
     */
    public void setupGame(int digits) {
        this.numberOfDigits = digits;
        this.cpuNumber = generateCpuNumber(digits); // 指定された桁数で正解を生成
        this.currentTurn = 0;
        this.history.clear();
        Log.d("HitBlow_GameManager", "CPU Number (Answer): " + cpuNumber);
    }

    /**
     * 正解の数字（CPUナンバー）を返します。
     *
     * @return 正解の数字列
     */
    public String getCpuNumber() {
        return cpuNumber;
    }

    /**
     * 0〜9の数字から重複のないランダムな数字列を生成します。
     *
     * @param digits 生成する桁数
     * @return 生成された数字列
     */
    private String generateCpuNumber(int digits) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            numbers.add(i);
        }
        // リストをシャッフルしてランダム性を確保
        Collections.shuffle(numbers);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            // シャッフルされたリストの先頭から必要な数だけ取り出す（重複回避）
            sb.append(numbers.remove(0));
        }
        return sb.toString();
    }

    /**
     * プレイヤーの入力を受け取り、ヒット（EAT）とブロー（BITE）の数を判定します。
     * 同時に回答履歴への保存とターン数のカウントアップを行います。
     * * @param playerGuess プレイヤーが入力した推測数字
     *
     * @return [ヒット数, ブロー数] の配列。入力が不正な場合は [-1, -1] を返します。
     */
    public int[] processCall(String playerGuess) {
        // バリデーションチェック
        if (!isValidGuess(playerGuess)) {
            return new int[]{-1, -1};
        }

        int eats = 0;
        int bites = 0;

        for (int i = 0; i < numberOfDigits; i++) {
            char guessChar = playerGuess.charAt(i);
            char cpuChar = cpuNumber.charAt(i);

            if (guessChar == cpuChar) {
                // 数字も位置も一致している場合
                eats++;
            } else if (cpuNumber.contains(String.valueOf(guessChar))) {
                // 数字は正解の中に含まれているが、位置が違う場合
                bites++;
            }
        }

        // 判定結果を履歴に記録
        this.currentTurn++;
        history.add(new HistoryEntry(currentTurn, playerGuess, eats, bites));

        return new int[]{eats, bites};
    }

    /**
     * プレイヤーの入力がゲームのルール（桁数の一致、数字の重複なし）に適合しているか検証します。
     *
     * @param guess プレイヤーの入力内容
     * @return 有効な入力であればtrue
     */
    private boolean isValidGuess(String guess) {
        // 桁数チェック
        if (guess.length() != numberOfDigits) return false;

        // 重複チェック（Setの特性を利用）
        Set<Character> uniqueDigits = new HashSet<>();
        for (char c : guess.toCharArray()) {
            if (!uniqueDigits.add(c)) return false;
        }
        return true;
    }

    // --- ゲッターメソッド群 ---

    /**
     * CPUナンバーが生成済みかどうかを確認します。
     */
    public boolean isCpuNumberSet() {
        return cpuNumber != null && !cpuNumber.isEmpty();
    }

    /**
     * 現在設定されている桁数を取得します。
     */
    public int getNumberOfDigits() {
        return numberOfDigits;
    }

    /**
     * 現在の経過ターン数を取得します。
     */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /**
     * これまでの全回答履歴を取得します。
     */
    public List<HistoryEntry> getHistory() {
        return history;
    }

    /**
     * 最新の回答結果（1つ前のターン）を取得します。
     *
     * @return 最新のHistoryEntry。履歴がない場合はnull。
     */
    public HistoryEntry getLastHistoryEntry() {
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }
}