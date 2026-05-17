package com.learnhub.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.learnhub.dto.*;
import com.learnhub.entity.User;
import com.learnhub.repository.UserRepository;
import com.learnhub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.valueOf(request.getRole().toUpperCase()))
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse loginWithGoogle(String credential) {
        try {
            System.out.println("GOOGLE LOGIN SERVICE HIT");
            System.out.println("Google credential received: " + (credential != null));

            GoogleIdToken.Payload payload = verifyGoogleToken(credential);
            String email = payload.getEmail();

            // LOGIN ONLY — user must already exist in DB
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_REGISTERED"));

            String token = jwtUtil.generateToken(user.getEmail());
            return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google login failed: " + e.getMessage());
        }
    }

    public AuthResponse registerWithGoogle(String credential, String role) {
        try {
            System.out.println("GOOGLE REGISTER SERVICE HIT");
            System.out.println("Google credential received: " + (credential != null));

            GoogleIdToken.Payload payload = verifyGoogleToken(credential);
            String email = payload.getEmail();
            String name  = (String) payload.get("name");

            if (userRepository.existsByEmail(email)) {
                // Fallback to login if user already exists
                User user = userRepository.findByEmail(email).get();
                String token = jwtUtil.generateToken(user.getEmail());
                return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
            }

            User.Role userRole = (role != null && role.equalsIgnoreCase("INSTRUCTOR"))
                    ? User.Role.INSTRUCTOR : User.Role.STUDENT;

            User user = User.builder()
                    .name(name != null ? name : email)
                    .email(email)
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(userRole)
                    .build();
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getEmail());
            return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google registration failed: " + e.getMessage());
        }
    }

    private GoogleIdToken.Payload verifyGoogleToken(String credential) throws Exception {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("GOOGLE_CLIENT_ID is not configured.");
        }
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId.trim()))
                .build();
        GoogleIdToken idToken = verifier.verify(credential);
        if (idToken == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        return idToken.getPayload();
    }
}
