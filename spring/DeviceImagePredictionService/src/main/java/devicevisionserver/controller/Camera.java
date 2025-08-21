package devicevisionserver.controller;

import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Camera {
    private static final String BACKEND_URL = "http://localhost:8080/api/receive";
    private static final String ESP32_BASE_URL = "http://192.168.1.100"; // ESP32的IP
    private static final String CAMERA_URL = ESP32_BASE_URL + "/capture";
    private static final Logger logger = LoggerFactory.getLogger(Camera.class);
    private static ScheduledExecutorService scheduler;
    private static int captureInterval = 2000;

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (Exception e) {
            logger.warn("OpenCV加载失败: {}", e.getMessage());
        }
    }

    public static void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.warn("摄像头服务已启动");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(Camera::captureAndSend, 0, captureInterval, TimeUnit.MILLISECONDS);
        logger.info("服务启动，间隔: {}ms", captureInterval);
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            logger.info("服务已停止");
        }
    }

    private static void captureAndSend() {
        try {
            byte[] imageData = fetchImage();
            if (imageData == null) return;

            String base64 = ImageUtil.convertToBase64(imageData);
            if (base64 == null) return;

            String deviceId = "esp32_cam_" + ESP32_BASE_URL.hashCode();
            sendToBackend(base64, deviceId);
        } catch (Exception e) {
            logger.error("处理失败: {}", e.getMessage());
        }
    }

    private static byte[] fetchImage() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(CAMERA_URL)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            logger.error("获取图像失败: {}", e.getMessage());
            return null;
        }
    }

    private static void sendToBackend(String base64Image, String deviceId) {
        String jsonBody = "{"
                + "\"username\":\"iot_device\","
                + "\"token\":\"valid_token\","
                + "\"deviceId\":\"" + deviceId + "\","
                + "\"imageData\":\"" + base64Image + "\""
                + "}";

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.info("发送成功，设备: {}", deviceId);
                handleBackendResponse(response.body(), deviceId);
            } else {
                logger.error("发送失败，状态: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.error("发送异常: {}", e.getMessage());
        }
    }

    private static void handleBackendResponse(String responseBody, String deviceId) {
        try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
            JsonObject responseJson = reader.readObject();
            if (!responseJson.containsKey("command")) return;

            JsonObject command = responseJson.getJsonObject("command");
            String type = command.getString("type");

            switch (type) {
                case "adjust_capture_interval":
                    adjustCaptureInterval(command.getInt("interval"));
                    break;
                case "set_resolution":
                    setResolution(command.getInt("width"), command.getInt("height"));
                    break;
                case "take_snapshot":
                    takeSnapshot();
                    break;
                default:
                    logger.warn("未知指令: {}", type);
            }
        } catch (Exception e) {
            logger.error("解析响应失败: {}", e.getMessage());
        }
    }

    private static void adjustCaptureInterval(int newInterval) {
        if (newInterval < 1000) {
            logger.warn("间隔不能小于1000ms");
            return;
        }
        captureInterval = newInterval;
        logger.info("间隔调整为: {}ms", newInterval);
    }

    private static void setResolution(int width, int height) {
        try {
            String url = ESP32_BASE_URL + "/set-resolution?width=" + width + "&height=" + height;
            HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.discarding());
            logger.info("请求调整分辨率: {}x{}", width, height);
        } catch (Exception e) {
            logger.error("调整分辨率失败: {}", e.getMessage());
        }
    }

    private static void takeSnapshot() {
        try {
            String url = ESP32_BASE_URL + "/take-snapshot";
            HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.discarding());
            logger.info("触发抓拍");
        } catch (Exception e) {
            logger.error("抓拍失败: {}", e.getMessage());
        }
    }

    private static class ImageUtil {
        private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

        public static String convertToBase64(byte[] imageData) {
            try {
                if (imageData.length > 10 * 1024 * 1024) {
                    logger.error("图片过大");
                    return null;
                }
                return Base64.getEncoder().encodeToString(imageData);
            } catch (Exception e) {
                logger.error("转Base64失败: {}", e.getMessage());
                return null;
            }
        }
    }
}