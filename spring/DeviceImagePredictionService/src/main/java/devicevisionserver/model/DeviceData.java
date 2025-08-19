package devicevisionserver.model;

// 用于接收发送的JSON数据
public class DeviceData {
    private String username;
    private String account;
    private String token;
    private String deviceId;
    private String imageData;

    // Getter和Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }
}
