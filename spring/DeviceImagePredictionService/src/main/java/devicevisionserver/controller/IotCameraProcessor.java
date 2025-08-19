package devicevisionserver.controller;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class IotCameraProcessor {
    //改接口
    private static final String BACKEND_URL = "http://你的服务器IP:8080/api/receive";
    private static final int CAMERA_INDEX = 0;

    private static final Logger logger = LoggerFactory.getLogger(IotCameraProcessor.class);

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        VideoCapture camera = new VideoCapture();
        try {
            camera.open(CAMERA_INDEX);
            if (!camera.isOpened()) {
                logger.error("无法打开摄像头，检查设备是否连接");
                return;
            }
            logger.info("摄像头连接成功，开始捕获图像...");

            Mat frame = new Mat();
            while (true) {
                // 捕获一帧图像
                boolean success = camera.read(frame);
                if (!success || frame.empty()) {
                    logger.warn("未捕获到图像帧，1秒后重试...");
                    Thread.sleep(1000);
                    continue;
                }

                byte[] rawImageData = matToByteArray(frame);
                if (rawImageData == null) {
                    logger.warn("无法提取图像数据，跳过当前帧");
                    continue;
                }


                String base64Image = ImageUtil.convertToBase64(rawImageData);
                if (base64Image != null) {
                    sendToBackend(base64Image, "opencv_cam_" + CAMERA_INDEX);
                }
                //半小时一张图
                Thread.sleep(2000 * 1000);
            }

        } catch (Exception e) {
            logger.error("设备处理异常", e);
        } finally {
            if (camera.isOpened()) {
                camera.release();
                logger.info("摄像头已关闭");
            }
        }
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
        String jsonBody = String.format("""
                {
                    "username": "iot_device",
                    "token": "valid_token",
                    "deviceId": "%s",
                    "imageData": "%s"
                }""", deviceId, base64Image);

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
            } else {
                logger.error("发送失败，状态码：{}", response.statusCode());
            }
        } catch (Exception e) {
            logger.error("发送请求异常", e);
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