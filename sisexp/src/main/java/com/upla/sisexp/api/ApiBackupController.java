package com.upla.sisexp.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('Administrador')")
public class ApiBackupController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @GetMapping("/backup")
    public ResponseEntity<?> backup() throws Exception {
        String host = extractDbHost(datasourceUrl);
        String dbName = extractDbName(datasourceUrl);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "sisexp_backup_" + timestamp + ".sql";

        Path tmpFile = Files.createTempFile("sisexp_backup_", ".sql");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", host,
                    "-U", dbUser,
                    "-d", dbName,
                    "--no-owner",
                    "--no-acl",
                    "--clean",
                    "--if-exists"
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);
            pb.redirectOutput(tmpFile.toFile());
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String err = new String(Files.readAllBytes(tmpFile), StandardCharsets.UTF_8);
                return ResponseEntity.internalServerError()
                        .body("Error en pg_dump (exit " + exitCode + "): " + err);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al ejecutar backup: " + e.getMessage());
        }

        byte[] content = Files.readAllBytes(tmpFile);
        tmpFile.toFile().delete();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''"
                                + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/sql"))
                .body(content);
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestParam("archivo") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Archivo vacio");
        }

        String host = extractDbHost(datasourceUrl);
        String dbName = extractDbName(datasourceUrl);
        Path tmpFile = null;

        try {
            tmpFile = Files.createTempFile("sisexp_restore_", ".sql");
            file.transferTo(tmpFile.toFile());

            ProcessBuilder pb = new ProcessBuilder(
                    "psql",
                    "-h", host,
                    "-U", dbUser,
                    "-d", dbName,
                    "-v", "ON_ERROR_STOP=1",
                    "-f", tmpFile.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.internalServerError()
                        .body("Error en psql (exit " + exitCode + "):\n" + output.toString());
            }

            return ResponseEntity.ok().body("Restauracion completada exitosamente.\n\n" + output.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al restaurar: " + e.getMessage());
        } finally {
            if (tmpFile != null) tmpFile.toFile().delete();
        }
    }

    private String extractDbHost(String url) {
        // jdbc:postgresql://host:5432/dbname
        String u = url.replace("jdbc:postgresql://", "");
        int slash = u.indexOf('/');
        if (slash > 0) u = u.substring(0, slash);
        int colon = u.lastIndexOf(':');
        if (colon > 0) return u.substring(0, colon);
        return u;
    }

    private String extractDbName(String url) {
        String u = url.replace("jdbc:postgresql://", "");
        int slash = u.lastIndexOf('/');
        if (slash >= 0) return u.substring(slash + 1);
        int q = u.indexOf('?');
        if (q > 0) return u.substring(0, q);
        return u;
    }
}
