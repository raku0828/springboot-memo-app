package com.example.memoapp;

import org.springframework.data.jpa.repository.JpaRepository;

// 「倉庫係」です。データベースへの出し入れは、この係が担当します。
// JpaRepository を extends（継承）するだけで、
// save（保存）・findAll（一覧）・deleteById（番号で削除）などの
// 基本の仕事が最初から使えます。自分で SQL を書かなくても、
// 裏側で JPA が SQL を作って DB に送ってくれます。
public interface MemoRepository extends JpaRepository<Memo, Long> {
}
