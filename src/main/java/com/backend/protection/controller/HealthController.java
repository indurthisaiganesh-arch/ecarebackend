package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("status", "UP");
        healthDetails.put("service", "Healthcare Patient Data Protection System");
        healthDetails.put("version", "1.0.0-SNAPSHOT");
        healthDetails.put("timestamp", Instant.now().toString());

        // Java Runtime Info
        Map<String, Object> runtimeInfo = new HashMap<>();
        runtimeInfo.put("javaVersion", System.getProperty("java.version"));
        runtimeInfo.put("javaVendor", System.getProperty("java.vendor"));
        runtimeInfo.put("osName", System.getProperty("os.name"));
        runtimeInfo.put("osArch", System.getProperty("os.arch"));
        Runtime runtime = Runtime.getRuntime();
        runtimeInfo.put("maxMemoryMb", runtime.maxMemory() / (1024 * 1024));
        runtimeInfo.put("totalMemoryMb", runtime.totalMemory() / (1024 * 1024));
        runtimeInfo.put("freeMemoryMb", runtime.freeMemory() / (1024 * 1024));
        healthDetails.put("runtime", runtimeInfo);

        // Database Connectivity Check
        Map<String, Object> dbInfo = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            dbInfo.put("status", "CONNECTED");
            dbInfo.put("databaseProductName", metaData.getDatabaseProductName());
            dbInfo.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            dbInfo.put("driverName", metaData.getDriverName());
            dbInfo.put("driverVersion", metaData.getDriverVersion());
            dbInfo.put("catalog", connection.getCatalog());

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS tableCount FROM information_schema.tables WHERE table_schema = 'public' ")) {
                if (rs.next()) {
                    dbInfo.put("tablesConfigured", rs.getInt("tableCount"));
                }
            }
            healthDetails.put("database", dbInfo);
            log.info("Health check requested: System and Database (MySQL) are UP and healthy.");
            return ResponseEntity.ok(ApiResponse.success(healthDetails, "System and MySQL database are operational"));
        } catch (Exception e) {
            log.error("Database connection failure during health check", e);
            dbInfo.put("status", "DOWN");
            dbInfo.put("error", e.getMessage());
            healthDetails.put("database", dbInfo);
            return ResponseEntity.status(503).body(ApiResponse.error(503, "Database connection unavailable", healthDetails));
        }
    }
}
