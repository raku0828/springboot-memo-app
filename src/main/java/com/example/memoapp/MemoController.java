package com.example.memoapp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// メモの API の窓口です。@RequestMapping("/api/memos") は
// 「このクラスの担当 URL は /api/memos から始まるもの」という意味です。
@RestController
@RequestMapping("/api/memos")
public class MemoController {

    // メモをためておくリスト（今はデータベースではなく、メモリの中だけです。
    // アプリを止めると消えます）
    private final List<Memo> memos = new ArrayList<>();

    // 番号を 1, 2, 3... と付けるためのカウンター
    private final AtomicLong counter = new AtomicLong();

    // GET /api/memos → メモの一覧を返す
    @GetMapping
    public List<Memo> list() {
        return memos;
    }

    // POST /api/memos → メモを1件追加する
    // @RequestBody は「送られてきた JSON を Memo オブジェクトに変換して受け取る」という目印です。
    @PostMapping
    public Memo add(@RequestBody Memo memo) {
        memo.setId(counter.incrementAndGet());
        memos.add(memo);
        return memo;
    }

    // DELETE /api/memos/1 → 番号 1 のメモを消す
    // @PathVariable は「URL の /1 の部分を受け取る」という目印です。
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        memos.removeIf(m -> m.getId() == id);
    }
}
