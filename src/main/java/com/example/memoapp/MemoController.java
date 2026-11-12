package com.example.memoapp;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// メモの API の窓口です。@RequestMapping("/api/memos") は
// 「このクラスの担当 URL は /api/memos から始まるもの」という意味です。
// メモの保管は倉庫係（MemoRepository）に任せて、データベースに保存します。
// だからアプリを再起動してもメモは消えません。
@RestController
@RequestMapping("/api/memos")
public class MemoController {

    // 倉庫係。コンストラクタで受け取る形（DI）にすると、
    // Spring Boot が起動時に用意して渡してくれます。
    private final MemoRepository repository;

    public MemoController(MemoRepository repository) {
        this.repository = repository;
    }

    // GET /api/memos → メモの一覧を返す（DB から取り出す）
    @GetMapping
    public List<Memo> list() {
        return repository.findAll();
    }

    // POST /api/memos → メモを1件追加する（DB に保存する。番号は DB が付けます）
    // @RequestBody は「送られてきた JSON を Memo オブジェクトに変換して受け取る」という目印です。
    @PostMapping
    public Memo add(@RequestBody Memo memo) {
        memo.setId(null); // 番号は DB に付けてもらうので、受け取った番号は使わない
        return repository.save(memo);
    }

    // DELETE /api/memos/1 → 番号 1 のメモを消す
    // @PathVariable は「URL の /1 の部分を受け取る」という目印です。
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        repository.deleteById(id);
    }
}
