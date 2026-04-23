package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.ChangePasswordDTO;
import com.thespawnpoint.backend.dto.ForgotPasswordDTO;
import com.thespawnpoint.backend.dto.LoginDTO;
import com.thespawnpoint.backend.dto.RegisterDTO;
import com.thespawnpoint.backend.dto.ResetPasswordDTO;
import com.thespawnpoint.backend.dto.VerifyEmailDTO;
import com.thespawnpoint.backend.entity.user.Profile;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileRepository profileRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Реєстрація успішна, перевірте email");

        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailDTO dto,
                                         HttpServletResponse response) {
        return ResponseEntity.ok(authService.verifyEmail(dto, response));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ForgotPasswordDTO dto) {
        return ResponseEntity.ok(authService.resendVerification(dto.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto,
                                   HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(dto, response));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (user == null) {
            body.put("authenticated", false);
            body.put("user", null);
            return ResponseEntity.ok(body);
        }

        String avatarUrl = profileRepository.findByUserId(user.getId())
                .map(Profile::getAvatarUrl)
                .orElse(null);

        body.put("authenticated", true);
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("displayName", user.getDisplayName());
        body.put("emailVerified", user.isEmailVerified());
        body.put("role", user.getRole().name());
        body.put("status", user.getStatus().name());
        body.put("lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : null);
        body.put("avatarUrl", avatarUrl);
        body.put("banned", user.isBanned());
        body.put("banReason", user.getBanReason());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        return ResponseEntity.ok(authService.logout(response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        return ResponseEntity.ok(authService.forgotPassword(dto));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        return ResponseEntity.ok(authService.resetPassword(dto));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user,
                                            @Valid @RequestBody ChangePasswordDTO dto) {
        return ResponseEntity.ok(authService.changePassword(user, dto));
    }
}