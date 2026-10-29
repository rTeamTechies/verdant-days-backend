package com.verdantdays.model.dto;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    public static class UserInfo {
        private Integer id;
        private String phoneNumber;
        private String name;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }
}
