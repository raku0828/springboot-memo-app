# Spring Boot サンプル（メモ API）

Spring Boot で作った、いちばん小さい Web API のサンプルです。
「URL にアクセスすると、Java のメソッドが呼ばれて答えが返る」という
Spring Boot の基本の流れを確認できます。

## できること（API 一覧）

| やること | URL | 説明 |
|---|---|---|
| あいさつ | GET /hello | "Hello, Spring Boot!" と返す |
| 名前つきあいさつ | GET /greet?name=raku | "こんにちは、raku さん！" と返す |
| メモ一覧 | GET /api/memos | ためたメモを全部返す |
| メモ追加 | POST /api/memos | JSON でメモを1件追加する |
| メモ削除 | DELETE /api/memos/{番号} | その番号のメモを消す |

※ メモはメモリの中にためているだけなので、アプリを止めると消えます（データベースはまだ使っていません）。

## 起動のしかた

必要なもの：JDK 21（このプロジェクトのフォルダで作業します）

```bash
./mvnw spring-boot:run
```

起動したら、ブラウザや curl で開きます。

```bash
curl http://localhost:8080/hello
curl "http://localhost:8080/greet?name=raku"
curl -X POST http://localhost:8080/api/memos -H "Content-Type: application/json" -d '{"title":"買い物","body":"牛乳を買う"}'
curl http://localhost:8080/api/memos
curl -X DELETE http://localhost:8080/api/memos/1
```

止めるときは Ctrl + C です。

## ファイル構成

```
springboot-memo-app/
├── pom.xml                     … 使う部品（ライブラリ）の一覧表
└── src/main/java/com/example/memoapp/
    ├── MemoappApplication.java … 起動用のクラス（main メソッドだけ）
    ├── HelloController.java    … /hello と /greet の窓口
    ├── MemoController.java     … /api/memos の窓口（一覧・追加・削除）
    └── Memo.java               … メモ1件の設計図（id・title・body）
└── src/main/resources/
    └── application.properties  … 設定ファイル（今は空っぽでOK）
```

## @（アノテーション）は「目印シール」

コードに出てくる `@○○` は、Spring Boot への目印シールです。
「このクラスは窓口です」「このメソッドはこの URL 担当です」のように、
**設定を文章ではなくシールで貼る**のが Spring Boot の書き方です。

| シール | 意味 |
|---|---|
| @RestController | このクラスは API の窓口です |
| @GetMapping("/hello") | GET で /hello に来たらこのメソッド |
| @PostMapping | POST（データを送る）で来たらこのメソッド |
| @DeleteMapping("/{id}") | DELETE で /api/memos/番号 に来たらこのメソッド |
| @RequestParam | URL の ?name=○○ を受け取る |
| @RequestBody | 送られてきた JSON をオブジェクトに変換して受け取る |
| @PathVariable | URL の /番号 の部分を受け取る |
| @RequestMapping("/api/memos") | このクラスの担当は /api/memos から始まる URL |

## 流れ（/hello の場合）

1. ブラウザが http://localhost:8080/hello にアクセスする
2. Spring Boot が「/hello 担当はどのメソッドか」をシールで探す
3. HelloController.hello() が呼ばれる
4. 返した文字列 "Hello, Spring Boot!" がそのままブラウザに返る

これが API の基本形です。現場の仕事では、ここに
「データベースから取る」「計算する」「チェックする」が増えていきます。

## データベースについて

メモは MySQL（互換の TiDB Cloud）に保存しています。再起動しても消えません。

- `Memo` クラスに付けた `@Entity` が「テーブルの設計図」の目印です
- 出し入れは「倉庫係」の `MemoRepository` が担当し、SQL は JPA が裏で作ります
- 接続先は環境変数 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` で渡します
  （パスワードをコードや GitHub に書かないためです）

## 次の一歩（やりたくなったら）

- 入力チェック（@Valid）を付ける
- 画面（HTML）を返すようにする
- 自分で SQL を書いて取り出す方法（JdbcTemplate や @Query）を試す
