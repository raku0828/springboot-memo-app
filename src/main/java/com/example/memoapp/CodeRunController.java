package com.example.memoapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// 学習サイト（Java学習メモ）の「コード実行」用窓口です。
//
// サイトの Worker が、ログイン済みユーザーの Java コードをここへ送ります。
// ここでは一時フォルダに Main.java を置いて javac でコンパイルし、
// java コマンドで実行して、標準出力＋標準エラーを JSON で返します。
//
// 安全のための制限：
// - 合言葉（X-Gateway-Key）が一致しないリクエストは拒否（呼び出しは Worker だけ）
// - 同時実行は2件まで。それ以上は「混雑」として断る
// - コンパイル15秒・実行5秒で強制終了（無限ループ対策）
// - 実行プロセスのメモリは最大128MB、出力は先頭8000文字まで
// - 子プロセスの環境変数は空にしてから最小限だけ渡す
//   （DB パスワードや合言葉がユーザーのコードから読めないようにするため）
@RestController
public class CodeRunController {

    private static final int MAX_CODE_CHARS = 20000;
    private static final int MAX_OUTPUT_CHARS = 8000;
    private static final long COMPILE_TIMEOUT_SEC = 15;
    private static final long RUN_TIMEOUT_SEC = 5;
    private static final Semaphore SLOTS = new Semaphore(2, true);
    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+class\\s+([A-Za-z_]\\w*)");

    private final String gatewayKey;

    public CodeRunController(@Value("${gateway.key:}") String gatewayKey) {
        this.gatewayKey = gatewayKey;
    }

    // リクエスト: { "code": "public class Main { ... }" }
    // レスポンス: { "ok": true/false, "stage": "compile"|"run"|"done",
    //               "output": "...", "exitCode": 0, "timedOut": false }
    @PostMapping("/gateway/run")
    public ResponseEntity<Map<String, Object>> run(
            @RequestHeader(value = "X-Gateway-Key", required = false) String key,
            @RequestBody Map<String, Object> body) {
        if (gatewayKey == null || gatewayKey.isEmpty() || !gatewayKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        String code = body.get("code") instanceof String s ? s : "";
        if (code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "コードが空です"));
        }
        if (code.length() > MAX_CODE_CHARS) {
            return ResponseEntity.badRequest().body(Map.of("error", "コードが長すぎます（" + MAX_CODE_CHARS + "文字まで）"));
        }
        if (!SLOTS.tryAcquire()) {
            return ResponseEntity.status(429).body(Map.of("error", "いま混雑しています。少し待ってから実行してください"));
        }
        Path dir = null;
        try {
            dir = Files.createTempDirectory("coderun-");
            String className = detectClassName(code);
            Path src = dir.resolve(className + ".java");
            Files.writeString(src, code, StandardCharsets.UTF_8);

            // ---- compile ----
            ProcResult comp = exec(dir, COMPILE_TIMEOUT_SEC,
                    javaBin("javac"), "-encoding", "UTF-8", "-d", dir.toString(), src.toString());
            if (comp.timedOut) {
                return ResponseEntity.ok(result(false, "compile", "コンパイルが" + COMPILE_TIMEOUT_SEC + "秒を超えたので止めました。", null, true));
            }
            if (comp.exitCode != 0) {
                // パス部分は見づらいので落として返す
                String out = comp.output.replace(dir.toString() + File.separator, "");
                return ResponseEntity.ok(result(false, "compile", out, comp.exitCode, false));
            }
            // ---- run ----
            ProcResult ran = exec(dir, RUN_TIMEOUT_SEC,
                    javaBin("java"), "-Xmx128m", "-Dfile.encoding=UTF-8", "-cp", dir.toString(), className);
            if (ran.timedOut) {
                String out = ran.output.isEmpty() ? "" : ran.output + "\n";
                return ResponseEntity.ok(result(false, "run",
                        out + "（" + RUN_TIMEOUT_SEC + "秒を超えたので止めました。無限ループになっていないか確認してください）", null, true));
            }
            return ResponseEntity.ok(result(ran.exitCode == 0, "done", ran.output, ran.exitCode, false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "実行サーバーでエラーが起きました: " + e.getMessage()));
        } finally {
            SLOTS.release();
            if (dir != null) {
                deleteRecursively(dir);
            }
        }
    }

    private Map<String, Object> result(boolean ok, String stage, String output, Integer exitCode, boolean timedOut) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok);
        m.put("stage", stage);
        m.put("output", output == null ? "" : output);
        m.put("exitCode", exitCode);
        m.put("timedOut", timedOut);
        return m;
    }

    private String detectClassName(String code) {
        Matcher m = PUBLIC_CLASS.matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        return "Main";
    }

    // javac / java の場所。JAVA_HOME があればそこから、なければ PATH 任せ。
    private String javaBin(String name) {
        String home = System.getenv("JAVA_HOME");
        if (home != null && !home.isEmpty()) {
            Path p = Path.of(home, "bin", name);
            if (Files.exists(p)) {
                return p.toString();
            }
        }
        return name;
    }

    private record ProcResult(int exitCode, String output, boolean timedOut) {}

    // 子プロセスを起動し、出力を読みながらタイムアウトを見張る。
    // 環境変数は空から始めて、必要最小限だけを渡す（秘密情報を渡さない）。
    private ProcResult exec(Path dir, long timeoutSec, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
        Map<String, String> env = pb.environment();
        String path = env.getOrDefault("PATH", "/usr/local/bin:/usr/bin:/bin");
        String javaHome = env.getOrDefault("JAVA_HOME", "");
        env.clear();
        env.put("PATH", path);
        if (!javaHome.isEmpty()) {
            env.put("JAVA_HOME", javaHome);
        }
        env.put("HOME", dir.toString());
        env.put("LANG", "C.UTF-8");
        Process proc = pb.start();

        StringBuilder buf = new StringBuilder();
        Thread drainer = new Thread(() -> {
            try (InputStream in = proc.getInputStream()) {
                byte[] chunk = new byte[4096];
                int n;
                while ((n = in.read(chunk)) != -1) {
                    synchronized (buf) {
                        if (buf.length() < MAX_OUTPUT_CHARS) {
                            String s = new String(chunk, 0, n, StandardCharsets.UTF_8);
                            buf.append(s, 0, Math.min(s.length(), MAX_OUTPUT_CHARS - buf.length()));
                        }
                    }
                }
            } catch (IOException ignored) {
                // プロセス終了時の読み取りエラーは無視する
            }
        });
        drainer.setDaemon(true);
        drainer.start();

        boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
            proc.waitFor(3, TimeUnit.SECONDS);
        }
        drainer.join(2000);
        int exit = finished ? proc.exitValue() : -1;
        return new ProcResult(exit, buf.toString(), !finished);
    }

    private void deleteRecursively(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
