package com.example.spring.controller;

import com.example.spring.pojo.User;
import com.example.spring.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @CrossOrigin(value = "http://localhost:9527")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        // 将username改为account
        String account = user.getAccount();
        String password = user.getPassword();

        if (account == null || password == null) {
            result.put("success", false);
            result.put("message", "账户或密码不能为空");
            return result;
        }

        // 调用login方法时使用account参数
        boolean success = userService.login(account, password);

        if (success) {
            // 生成token时使用account
            String token = generateToken(account);
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("code", 20000);
            result.put("token", token);
        } else {
            result.put("success", false);
            result.put("message", "账户或密码错误");
            result.put("code", 50000);
        }

        return result;
    }

    @CrossOrigin(value = "http://localhost:9527")
    @GetMapping("/info")
    public Map<String, Object> getInfo(@RequestHeader("X-Token") String token) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 从token中解析出account
            String account = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            // 根据account判断角色
            String[] roles = "admin".equals(account) ? new String[]{"admin"} : new String[]{"editor"};

            Map<String, Object> data = new HashMap<>();
            data.put("roles", roles);
            data.put("name", account);
            data.put("avatar", "https://picsum.photos/200/200");

            result.put("success", true);
            result.put("code", 20000);
            result.put("data", data);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Token无效或已过期");
            result.put("code", 50008);
        }

        return result;
    }

    @CrossOrigin(value = "http://localhost:9527")
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", 20000);
        result.put("message", "登出成功");
        return result;
    }

    @CrossOrigin(value = "http://localhost:9527")
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        // 将username改为account
        String account = user.getAccount();
        String password = user.getPassword();

        if (account == null || password == null || account.isEmpty() || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "账户或密码不能为空");
            return result;
        }

        // 检查账户是否存在，使用existsAccount方法
        if (userService.existsAccount(account)) {
            result.put("success", false);
            result.put("message", "账户已被注册");
            return result;
        }

        boolean success = userService.register(user);
        if (success) {
            result.put("success", true);
            result.put("code", 20000);
            result.put("message", "注册成功");
        } else {
            result.put("success", false);
            result.put("message", "注册失败");
        }

        return result;
    }

    private String generateToken(String account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", account);
        claims.put("iat", new Date());

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
