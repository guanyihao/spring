package com.example.spring.service.impl;

import com.example.spring.mapper.UserMapper;
import com.example.spring.pojo.User;
import com.example.spring.service.UserService;
import com.example.spring.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;


    @Override
    public boolean login(String account, String password) {
        User user = userMapper.selectByAccount(account);
        if (user == null) {
            return false;
        }
        String encryptedPassword = MD5Util.encrypt(password);
        return encryptedPassword.equals(user.getPassword());
    }

    @Override
    public boolean existsUsername(String username) {
        return false;
    }

    @Override
    public boolean existsAccount(String account) {
        User user = userMapper.selectByAccount(account);
        return user != null;
    }

    @Override
    public boolean register(User user) {
        if (existsAccount(user.getAccount())) {
            throw new RuntimeException("账户已被注册");
        }

        String encryptedPassword = MD5Util.encrypt(user.getPassword());
        user.setPassword(encryptedPassword);

        long currentTime = System.currentTimeMillis();
        user.setCreateTime(currentTime);
        user.setModifyTime(currentTime);

        int rows = userMapper.insert(user);
        return rows > 0;
    }
}