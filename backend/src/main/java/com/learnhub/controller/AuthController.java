package com.learnhub.controller;

import com.learnhub.dto.*;
import com.learnhub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.loginWithGoogle(body.get("credential")));
    }

    @PostMapping("/google/register")
    public ResponseEntity<AuthResponse> googleRegister(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.registerWithGoogle(body.get("credential"), body.get("role")));
    }
}
