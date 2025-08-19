package devicevisionserver.controller;

import devicevisionserver.ImageRecognitionService;
import devicevisionserver.model.DeviceData;
import devicevisionserver.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    private static final String[] REQUIRED_FIELDS = {"username", "token", "deviceId", "imageData"};

    @Autowired
    private ImageRecognitionService imageRecognitionService;

    @Value("${image.storage.path:./uploaded-images}")
    private String imageStoragePath;

    @PostMapping("/receive")
    public Response handlePrediction(@RequestBody DeviceData requestData) {
        logger.info("收到来自设备 {} 的请求，用户名：{}",
                requestData.getDeviceId(),
                requestData.getUsername());

        StringBuilder missingFields = new StringBuilder();
        for (String field : REQUIRED_FIELDS) {
            String value = getFieldValue(requestData, field);
            if (isEmpty(value)) {
                missingFields.append(field).append(", ");
            }
        }
        if (!missingFields.isEmpty()) {
            String errorMsg = "缺少必填字段：" + missingFields.substring(0, missingFields.length() - 2);
            logger.warn("请求参数错误：{}", errorMsg);
            return new Response(400, errorMsg);
        }

        try {
            // 处理图片
            String base64Image = requestData.getImageData();
            String cleanBase64 = base64Image.replaceAll("^data:image/.*;base64,", "");
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // 保存图片到本地
            String savedFilePath = saveImage(imageBytes, requestData.getDeviceId());
            logger.info("图片保存成功，路径：{}", savedFilePath);

            // 调用识别服务
            String recognitionResult = imageRecognitionService.recognize(imageBytes);

            return new Response(200, "处理成功！识别结果：" + recognitionResult + "，图片路径：" + savedFilePath);

        } catch (IllegalArgumentException e) {
            logger.error("Base64解码失败", e);
            return new Response(400, "图片格式错误：无效的Base64编码");
        } catch (IOException e) {
            logger.error("图片处理失败", e);
            return new Response(500, "图片保存失败：" + e.getMessage());
        } catch (Exception e) {
            logger.error("请求处理异常", e);
            return new Response(500, "服务器处理失败：" + e.getMessage());
        }
    }


    private String saveImage(byte[] imageBytes, String deviceId) throws IOException {
        String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        Path storagePath = Paths.get(imageStoragePath, deviceId, dateDir);
        Files.createDirectories(storagePath);

        String fileName = UUID.randomUUID() + ".jpg";
        Path filePath = storagePath.resolve(fileName);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("无法解析为有效图片格式");
            }
            ImageIO.write(image, "jpg", filePath.toFile());
        }

        return filePath.toString();
    }

    private String getFieldValue(DeviceData data, String field) {
        if (data == null) {
            return null;
        }
        return switch (field) {
            case "username" -> data.getUsername();
            case "token" -> data.getToken();
            case "deviceId" -> data.getDeviceId();
            case "imageData" -> data.getImageData();
            default -> null;
        };
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
