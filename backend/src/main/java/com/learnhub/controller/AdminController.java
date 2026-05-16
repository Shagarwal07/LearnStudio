package com.learnhub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DataSource dataSource;

    // ── Execute any SQL (SELECT → rows, others → affected rows) ──
    /*
    @PostMapping("/sql")
    public ResponseEntity<Map<String, Object>> runSql(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        if (sql == null || sql.isBlank()) return ResponseEntity.badRequest().build();

        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean isSelect = sql.trim().toUpperCase().startsWith("SELECT") ||
                               sql.trim().toUpperCase().startsWith("SHOW") ||
                               sql.trim().toUpperCase().startsWith("DESCRIBE") ||
                               sql.trim().toUpperCase().startsWith("EXPLAIN");

            if (isSelect) {
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= cols; i++) columns.add(meta.getColumnName(i));

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) row.add(rs.getObject(i));
                    rows.add(row);
                }
                result.put("type", "SELECT");
                result.put("columns", columns);
                result.put("rows", rows);
                result.put("rowCount", rows.size());
            } else {
                int affected = stmt.executeUpdate(sql);
                result.put("type", "UPDATE");
                result.put("affectedRows", affected);
                result.put("message", "Query executed successfully. " + affected + " row(s) affected.");
            }

            result.put("executionMs", System.currentTimeMillis() - start);
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (SQLException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("executionMs", System.currentTimeMillis() - start);
            return ResponseEntity.ok(result);
        }
    }
    */

    // ── List all tables ──────────────────────────────────────
    /*
    @GetMapping("/tables")
    public ResponseEntity<List<Map<String, Object>>> getTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("name", rs.getString("TABLE_NAME"));
                // get row count
                try (Statement s = conn.createStatement();
                     ResultSet cr = s.executeQuery("SELECT COUNT(*) FROM `" + rs.getString("TABLE_NAME") + "`")) {
                    cr.next();
                    t.put("rowCount", cr.getLong(1));
                }
                tables.add(t);
            }
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(tables);
    }
    */

    // ── DB Stats ─────────────────────────────────────────────
    /*
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stats.put("users",       queryCount(stmt, "SELECT COUNT(*) FROM users"));
            stats.put("courses",     queryCount(stmt, "SELECT COUNT(*) FROM courses"));
            stats.put("lessons",     queryCount(stmt, "SELECT COUNT(*) FROM lessons"));
            stats.put("enrollments", queryCount(stmt, "SELECT COUNT(*) FROM enrollments"));
            stats.put("students",    queryCount(stmt, "SELECT COUNT(*) FROM users WHERE role='STUDENT'"));
            stats.put("instructors", queryCount(stmt, "SELECT COUNT(*) FROM users WHERE role='INSTRUCTOR'"));
        } catch (SQLException e) {
            stats.put("error", e.getMessage());
        }
        return ResponseEntity.ok(stats);
    }
    */

    private long queryCount(Statement stmt, String sql) throws SQLException {
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        return rs.getLong(1);
    }
}
