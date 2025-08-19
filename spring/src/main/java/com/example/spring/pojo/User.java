package com.example.spring.pojo;

import java.io.Serializable;

public class User implements Serializable {
    private Long accountId;     // 用户ID，12位以内
    private String account;     // 用户账户，16字符以内
    private String password;    // 用户密码，16字符以内（MD5加密）
    private String name;        // 用户姓名
    private String gender;      // 性别：1-男，0-女
    private String email;       // 新增：用户邮箱
    private String phone;       // 新增：手机号码
    private String birthday;    // 新增：生日（格式：yyyy-MM-dd）
    private String token;       // 令牌：密码+登录时间
    private Long loginTime;     // 登录时间
    private Long createTime;    // 创建时间
    private Long modifyTime;    // 修改时间
    private String loginIP;     // 登录IP


    public User() {}

    public User(String account, String password) {
        this.account = account;
        this.password = password;
    }


    public User(Long accountId, String account, String password, String name, String gender, String email,
                String phone, String birthday, String token, Long loginTime, Long createTime,
                Long modifyTime, String loginIP) {
        this.accountId = accountId;
        this.account = account;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.birthday = birthday;
        this.token = token;
        this.loginTime = loginTime;
        this.createTime = createTime;
        this.modifyTime = modifyTime;
        this.loginIP = loginIP;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    // 新增email的getter/setter
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }


    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getLoginTime() { return loginTime; }
    public void setLoginTime(Long loginTime) { this.loginTime = loginTime; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public Long getModifyTime() { return modifyTime; }
    public void setModifyTime(Long modifyTime) { this.modifyTime = modifyTime; }

    public String getLoginIP() { return loginIP; }
    public void setLoginIP(String loginIP) { this.loginIP = loginIP; }

    @Override
    public String toString() {
        return "User{" +
                "accountId=" + accountId +
                ", account='" + account + '\'' +
                ", password='******'" +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", birthday='" + birthday + '\'' +
                ", token='" + token + '\'' +
                ", loginTime=" + loginTime +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                ", loginIP='" + loginIP + '\'' +
                '}';
    }
}