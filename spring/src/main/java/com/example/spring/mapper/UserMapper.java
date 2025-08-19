package com.example.spring.mapper;

import com.example.spring.pojo.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    // 1. 根据账户查询用户（用于登录和检查账户是否存在）
    User selectByAccount(@Param("account") String account);

    // 2. 插入新用户（用于注册功能）
    int insert(User user);
}
