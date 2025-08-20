package devicevisionserver.controller;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
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

public class Camera {
    // 后端接口地址
    private static final String BACKEND_URL = "http://localhost:8080/api/receive";

    // 摄像头URL
    private static final String CAMERA_URL = "";
    private static final Logger logger = LoggerFactory.getLogger(Camera.class);
    private static VideoCapture camera;
    private static int captureInterval = 2000;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static String hideSensitiveInfo(String url) {
        if (url.contains("@")) {
            return url.replaceAll("://.*@", "://[隐藏账号密码]@");
        }
        return url;
    }

    private static byte[] matToByteArray(Mat mat) {
        try {
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, matOfByte);
            return matOfByte.toArray();
        } catch (Exception e) {
            logger.error("Mat转字节数组失败", e);
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

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.info("图像发送成功，设备ID：{}", deviceId);
                handleBackendResponse(response.body(), deviceId);
            } else {
                logger.error("发送失败，状态码：{}，响应内容：{}",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("发送请求异常", e);
        }
    }


    private static void handleBackendResponse(String responseBody, String deviceId) {
        try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
            JsonObject responseJson = reader.readObject();

            int code = responseJson.getInt("code");
            String message = responseJson.getString("message");
            logger.info("后端响应 - 设备ID: {}, 状态码: {}, 消息: {}", deviceId, code, message);

            if (responseJson.containsKey("command")) {
                JsonObject command = responseJson.getJsonObject("command");
                String commandType = command.getString("type");
                logger.info("收到后端指令: {}", commandType);

                switch (commandType) {
                    case "adjust_capture_interval":
                        int newInterval = command.getInt("interval");
                        adjustCaptureInterval(newInterval);
                        break;

                    case "set_resolution":
                        int width = command.getInt("width");
                        int height = command.getInt("height");
                        setCameraResolution(width, height);
                        break;

                    case "take_snapshot":
                        takeImmediateSnapshot();
                        break;

                    default:
                        logger.warn("未知指令类型: {}", commandType);
                }
            }
        } catch (Exception e) {
            logger.error("解析后端响应失败", e);
        }
    }

    private static void adjustCaptureInterval(int newInterval) {
        if (newInterval < 1000) {
            logger.warn("捕获间隔不能小于1000毫秒，忽略指令");
            return;
        }
        captureInterval = newInterval;
        logger.info("已调整捕获间隔为: {}毫秒", captureInterval);
    }

    private static void setCameraResolution(int width, int height) {
        if (camera == null || !camera.isOpened()) {
            logger.error("摄像头未连接，无法设置分辨率");
            return;
        }
        boolean widthSet = camera.set(3, width);  // CV_CAP_PROP_FRAME_WIDTH = 3
        boolean heightSet = camera.set(4, height); // CV_CAP_PROP_FRAME_HEIGHT = 4

        if (widthSet && heightSet) {
            logger.info("已设置摄像头分辨率为: {}x{}", width, height);
        } else {
            logger.warn("无法设置分辨率为{}x{}，可能不被摄像头支持", width, height);
        }
    }

    private static void takeImmediateSnapshot() {
        try {
            logger.info("执行立即抓拍指令");
            Mat snapshot = new Mat();
            boolean success = camera.read(snapshot);
            if (success && !snapshot.empty()) {
                byte[] imageData = matToByteArray(snapshot);
                if (imageData != null) {
                    String base64 = ImageUtil.convertToBase64(imageData);
                    if (base64 != null) {
                        String deviceId = "net_cam_" + CAMERA_URL.hashCode();
                        sendToBackend(base64, deviceId + "_snapshot");
                    }
                }
            } else {
                logger.error("抓拍失败，无法获取图像帧");
            }
        } catch (Exception e) {
            logger.error("执行抓拍指令异常", e);
        }
    }

    private static class ImageUtil {
        private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

        public static String convertToBase64(byte[] imageData) {
            try {
                if (imageData.length > 10 * 1024 * 1024) {
                    logger.error("图片过大，超过10MB限制");
                    return null;
                }
                return Base64.getEncoder().encodeToString(imageData);
            } catch (Exception e) {
                logger.error("字节数据转Base64失败", e);
                return null;
            }
        }
    }
}
