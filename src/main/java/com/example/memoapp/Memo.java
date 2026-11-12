package com.example.memoapp;

// メモ1件分のデータを入れる「設計図」（クラス）です。
// 学習サイトで学んだ「クラス＝設計図、オブジェクト＝実物」と同じ考え方です。
public class Memo {

    private Long id;      // 番号（自動で付きます）。受け取った直後は空（null）でもいいように Long にしています
    private String title; // タイトル
    private String body;  // 中身

    // 空のコンストラクタ（JSON から受け取るときに必要です）
    public Memo() {
    }

    public Memo(Long id, String title, String body) {
        this.id = id;
        this.title = title;
        this.body = body;
    }

    // getter / setter は「中身を読んだり書き換えたりするための窓口」です。
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
