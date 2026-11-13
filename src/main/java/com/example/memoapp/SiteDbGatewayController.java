package com.example.memoapp;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// 学習サイト（Java学習メモ）専用の「SQL 中継」窓口です。
//
// サイト本体は Cloudflare で動いていて、そこから直接 MySQL にはつなげません。
// そこでサイトの Worker がこの窓口に SQL を送り、ここが代わりに
// java_memo_db（TiDB Cloud）へ実行して、結果を JSON で返します。
// 合言葉（X-Gateway-Key ヘッダ）が一致しないリクエストは断ります。
@RestController
public class SiteDbGatewayController {

    private final JdbcTemplate siteDb;
    private final String gatewayKey;

    public SiteDbGatewayController(
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPass,
            @Value("${gateway.key:}") String gatewayKey) {
        // メモ用 DB と同じサーバー・同じ鍵で、DB 名だけ java_memo_db に替える
        String siteUrl = dbUrl.replaceFirst("/memo_db(?=\\?|$)", "/java_memo_db");
        DataSource ds = new DriverManagerDataSource(siteUrl, dbUser, dbPass);
        this.siteDb = new JdbcTemplate(ds);
        this.gatewayKey = gatewayKey;
    }

    // リクエスト: { "sql": "SELECT ... WHERE id = ?", "params": [1] }
    // レスポンス: { "rows": [...], "insertId": 12, "affected": 1 }
    @PostMapping("/gateway/sql")
    public ResponseEntity<Map<String, Object>> sql(
            @RequestHeader(value = "X-Gateway-Key", required = false) String key,
            @RequestBody Map<String, Object> body) {
        if (gatewayKey == null || gatewayKey.isEmpty() || !gatewayKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        String sql = body.get("sql") instanceof String s ? s.trim() : "";
        if (sql.isEmpty() || sql.contains(";")) {
            return ResponseEntity.badRequest().body(Map.of("error", "bad sql"));
        }
        Object[] params = toParams(body.get("params"));
        String head = sql.split("\\s+", 2)[0].toUpperCase();
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            if (head.equals("SELECT") || head.equals("WITH")) {
                List<Map<String, Object>> rows = siteDb.queryForList(sql, params);
                out.put("rows", rows);
                out.put("affected", rows.size());
                out.put("insertId", null);
            } else if (head.equals("INSERT") || head.equals("REPLACE")) {
                KeyHolder kh = new GeneratedKeyHolder();
                int n = siteDb.update(con -> {
                    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                    return ps;
                }, kh);
                Number k = kh.getKey();
                out.put("rows", List.of());
                out.put("affected", n);
                out.put("insertId", k == null ? null : k.longValue());
            } else {
                int n = siteDb.update(sql, params);
                out.put("rows", List.of());
                out.put("affected", n);
                out.put("insertId", null);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", String.valueOf(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    private Object[] toParams(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return new Object[0];
        }
        List<Object> out = new ArrayList<>(list.size());
        for (Object v : list) {
            // JSON の数値は Integer/Long/Double で届く。DB へはそのまま渡す
            out.add(v);
        }
        return out.toArray();
    }
}
