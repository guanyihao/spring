package devicevisionserver;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImageRecognitionService {

    public String processImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "未接收到图片";
        }

        return String.format("图片接收成功！文件名：%s，大小：%d字节",
                file.getOriginalFilename(),
                file.getSize());
    }

    public String recognize(byte[] imageBytes) {
        return "当前未启用识别模型，图片已成功接收并保存";
    }
}