//package com.example.AI.Hotel.config;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.event.EventListener;
//import jakarta.annotation.PreDestroy;
//
//import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//@Configuration
//public class PythonProcessConfig {
//
//    private static final Logger logger = LoggerFactory.getLogger(PythonProcessConfig.class);
//    private Process pythonProcess;
//    private File tempPythonFile;
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void startPythonProcess() {
//        try {
//            // Lấy file embedding_api.py từ resources
//            InputStream pythonScriptStream = getClass().getClassLoader().getResourceAsStream("models/embedding_api.py");
//            if (pythonScriptStream == null) {
//                throw new FileNotFoundException("embedding_api.py not found in resources/models");
//            }
//
//            // Tạo file tạm thời để chạy
//            tempPythonFile = File.createTempFile("embedding_api", ".py");
//            Files.copy(pythonScriptStream, tempPythonFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//            pythonScriptStream.close();
//
//            // Đảm bảo file tạm thời có quyền thực thi (nếu cần)
//            tempPythonFile.setExecutable(true);
//
//            // Lệnh chạy file Python
//            ProcessBuilder processBuilder = new ProcessBuilder("python3", tempPythonFile.getAbsolutePath());
//            processBuilder.redirectErrorStream(true);
//
//            pythonProcess = processBuilder.start();
//
//            new Thread(() -> {
//                try (var reader = pythonProcess.getInputStream()) {
//                    byte[] buffer = new byte[1024];
//                    int bytesRead;
//                    while ((bytesRead = reader.read(buffer)) != -1) {
//                        String output = new String(buffer, 0, bytesRead);
//                        logger.info("Python process output: {}", output);
//                    }
//                } catch (IOException e) {
//                    logger.error("Error reading Python process output", e);
//                }
//            }).start();
//
//            logger.info("Python embedding API started");
//
//        } catch (IOException e) {
//            logger.error("Failed to start Python process", e);
//            throw new RuntimeException("Failed to start Python embedding API", e);
//        }
//    }
//
//    @PreDestroy
//    public void stopPythonProcess() {
//        if (pythonProcess != null && pythonProcess.isAlive()) {
//            pythonProcess.destroy();
//            try {
//                pythonProcess.waitFor();
//                logger.info("Python process stopped");
//            } catch (InterruptedException e) {
//                logger.error("Error stopping Python process", e);
//                pythonProcess.destroyForcibly();
//            }
//        }
//        // Xóa file tạm thời
//        if (tempPythonFile != null && tempPythonFile.exists()) {
//            tempPythonFile.delete();
//        }
//    }
//}