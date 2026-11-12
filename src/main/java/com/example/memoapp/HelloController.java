package com.example.memoapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController は「これは API の窓口です」という目印シールです。
// このクラスに書いたメソッドが、URL へのアクセスに答えます。
@RestController
public class HelloController {

    // @GetMapping("/hello") は「/hello という URL で来たら、このメソッドを呼ぶ」という目印です。
    // ブラウザで http://localhost:8080/hello を開くと、下の文字がそのまま表示されます。
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // @RequestParam は「URL の ?name=○○ の部分を受け取る」という目印です。
    // 例：http://localhost:8080/greet?name=raku → "こんにちは、raku さん！"
    // 何も指定がなければ "ゲスト" になります（defaultValue）。
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "ゲスト") String name) {
        return "こんにちは、" + name + " さん！";
    }
}
