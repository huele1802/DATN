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
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.concurrent.TimeUnit;
//
//@Configuration
//public class PythonProcessConfig {
//
//    private static final Logger logger = LoggerFactory.getLogger(PythonProcessConfig.class);
//    private Process pythonProcess;
//    private File tempPythonFile;
//    private static final int FASTAPI_PORT = 8000;
//    private static final int STARTUP_TIMEOUT_SECONDS = 60; // Thời gian chờ FastAPI khởi động
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void startPythonProcess() {
//        try {
//            // Kiểm tra xem python3 có sẵn trong hệ thống không
//            if (!isPythonAvailable()) {
//                throw new RuntimeException("Python3 is not available in the system. Please ensure 'python3' is installed and added to PATH.");
//            }
//
//            // Kiểm tra port 8000
//            if (!isPortAvailable(FASTAPI_PORT)) {
//                throw new RuntimeException("Port " + FASTAPI_PORT + " is already in use. Please free the port or configure a different port in embedding_api.py.");
//            }
//
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
//            processBuilder.redirectErrorStream(true); // Gộp stdout và stderr
//
//            // Redirect output và error của tiến trình Python vào log
//            File logFile = new File("python-embedding-api.log");
//            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
//            processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(logFile));
//
//            pythonProcess = processBuilder.start();
//
//            // Chờ FastAPI khởi động thành công
//            boolean isFastApiRunning = waitForFastApiStartup(STARTUP_TIMEOUT_SECONDS);
//            if (!isFastApiRunning) {
//                throw new RuntimeException("FastAPI server failed to start within " + STARTUP_TIMEOUT_SECONDS + " seconds. Check python-embedding-api.log for details.");
//            }
//
//            logger.info("Python embedding API started successfully on port {}", FASTAPI_PORT);
//
//        } catch (IOException | InterruptedException e) {
//            logger.error("Failed to start Python process", e);
//            throw new RuntimeException("Failed to start Python embedding API", e);
//        }
//    }
//
//    @PreDestroy
//    public void stopPythonProcess() {
//        try {
//            if (pythonProcess != null && pythonProcess.isAlive()) {
//                logger.info("Stopping Python process...");
//                pythonProcess.destroy();
//                boolean terminated = pythonProcess.waitFor(5, TimeUnit.SECONDS);
//                if (!terminated) {
//                    logger.warn("Python process did not terminate gracefully, forcing termination...");
//                    pythonProcess.destroyForcibly();
//                }
//                logger.info("Python process stopped successfully");
//            }
//        } catch (InterruptedException e) {
//            logger.error("Error stopping Python process", e);
//            Thread.currentThread().interrupt(); // Khôi phục trạng thái interrupt
//        } finally {
//            // Xóa file tạm thời
//            if (tempPythonFile != null && tempPythonFile.exists()) {
//                boolean deleted = tempPythonFile.delete();
//                if (!deleted) {
//                    logger.warn("Failed to delete temporary Python file: {}", tempPythonFile.getAbsolutePath());
//                }
//            }
//        }
//    }
//
//    // Kiểm tra xem Python có sẵn trong hệ thống không
//    private boolean isPythonAvailable() {
//        try {
//            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
//            Process process = pb.start();
//            int exitCode = process.waitFor();
//            return exitCode == 0;
//        } catch (IOException | InterruptedException e) {
//            logger.warn("Python3 is not available in the system", e);
//            return false;
//        }
//    }
//
//    // Kiểm tra xem port có sẵn không
//    private boolean isPortAvailable(int port) {
//        try {
//            URL url = new URL("http://localhost:" + port);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setConnectTimeout(1000);
//            connection.connect();
//            connection.disconnect();
//            return false; // Kết nối thành công, port đang được sử dụng
//        } catch (IOException e) {
//            return true; // Không kết nối được, port còn trống
//        }
//    }
//
//    // Chờ FastAPI khởi động
//    private boolean waitForFastApiStartup(int timeoutSeconds) throws InterruptedException {
//        long startTime = System.currentTimeMillis();
//        long timeoutMillis = timeoutSeconds * 1000L;
//
//        while (System.currentTimeMillis() - startTime < timeoutMillis) {
//            if (!pythonProcess.isAlive()) {
//                logger.error("Python process exited unexpectedly");
//                return false;
//            }
//
//            try {
//                URL url = new URL("http://localhost:" + FASTAPI_PORT);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setConnectTimeout(1000);
//                connection.setRequestMethod("GET");
//                connection.connect();
//                int responseCode = connection.getResponseCode();
//                connection.disconnect();
//
//                if (responseCode == 200 || responseCode == 404) { // FastAPI trả về 200 hoặc 404 là đã chạy
//                    logger.info("FastAPI server started successfully");
//                    return true;
//                }
//            } catch (IOException e) {
//                // Port chưa sẵn sàng, tiếp tục chờ
//            }
//
//            Thread.sleep(1000); // Chờ 1 giây trước khi thử lại
//        }
//
//        return false; // Timeout
//    }
//}