package com.example.spring.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {

    public static String encrypt(String rawStr) {
        try {
            // 获取MD5加密实例
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 对字符串进行加密
            byte[] bytes = md.digest(rawStr.getBytes(StandardCharsets.UTF_8));
            // 转换为16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append("0");
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
}