package com.example.memoapp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// メモ1件分のデータを入れる「設計図」（クラス）です。
// 学習サイトで学んだ「クラス＝設計図、オブジェクト＝実物」と同じ考え方です。
//
// @Entity は「このクラスはデータベースのテーブルと対応します」という目印です。
// このクラスは memo テーブルになり、id / title / body がその列になります。
@Entity
public class Memo {

    @Id // この項目が「主キー（行を見分ける番号）」です
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 番号は DB が自動で付けます（AUTO_INCREMENT と同じ）
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
