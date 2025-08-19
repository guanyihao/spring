package com.example.spring.service;

import com.example.spring.pojo.User;

public interface UserService {
    boolean login(String username, String password);

    boolean existsUsername(String username);

    boolean existsAccount(String account);

    boolean register(User user);
}