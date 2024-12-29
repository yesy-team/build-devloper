package com.yesy.team.build_devloper.security.custom.controller;

import com.yesy.team.build_devloper.security.jwt.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2StatusController {

    private final JwtUtil jwtUtil;

    public OAuth2StatusController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/status")
    public ResponseEntity<?> checkAuthStatus(@CookieValue(name = "accessToken", required = false) String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No token provided"));
        }

        try {
            jwtUtil.validateToken(accessToken);
            return ResponseEntity.ok(Map.of("message", "Authenticated"));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }
}

