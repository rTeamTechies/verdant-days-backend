package com.verdantdays.controller;

import com.verdantdays.model.AdminUser;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.LoginRequest;
import com.verdantdays.model.dto.LoginResponse;
import com.verdantdays.repository.AdminUserRepository;
import com.verdantdays.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getPhoneNumber() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number and password are required"));
        }

        String phone = request.getPhoneNumber().trim().replaceAll("\\s", "");
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(phone, request.getPassword())
        );

        AdminUser user = adminUserRepository.findByPhoneNumber(phone).orElseThrow();
        user.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(user);

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(phone);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        userInfo.setName(user.getName());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setRefreshToken(refreshToken);
        response.setUser(userInfo);

        return ResponseEntity.ok(response);
    }
}
