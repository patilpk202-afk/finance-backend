package finance.manager.controller;

import finance.manager.model.User;
import finance.manager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.util.Optional;
import java.util.Random;

import finance.manager.model.PasswordResetToken;
import finance.manager.repository.PasswordResetTokenRepository;
import finance.manager.service.EmailService;
import finance.manager.exception.CustomException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    // ✅ Register
    @PostMapping("/register")
    public User register(@Valid @RequestBody User user) {
        return service.register(user);
    }

    // ✅ Login
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User user) {

        String token = service.login(user.getEmail(), user.getPassword());

        return Map.of("token", token);
    }

    // ✅ Delete Account
    @DeleteMapping("/delete-account")
    public Map<String, String> deleteAccount(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        service.deleteAccount(userId);
        return Map.of("message", "Account deleted successfully");
    }

    // ✅ Send OTP (Forgot Password Request)
    @PostMapping("/send-otp")
    public Map<String, String> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null) throw new CustomException("Email is required");

        // Verify that the user exists organically
        finance.manager.model.User existingUser = service.findByEmail(email);
        if (existingUser == null) {
            throw new CustomException("No account registered with this email address");
        }

        // Clear existing OTP arrays blocking spam constraints
        tokenRepository.deleteByEmail(email);

        // Generate bounds
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 5); // 5 minute TTL

        PasswordResetToken token = new PasswordResetToken(email, otp, cal.getTime());
        tokenRepository.save(token);

        try {
            emailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            throw new CustomException("Failed to dispatch Email: Check Configuration Credentials");
        }

        return Map.of("message", "OTP sent successfully");
    }

    // ✅ Verify OTP bounds
    @PostMapping("/verify-otp")
    public Map<String, String> verifyOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        PasswordResetToken token = tokenRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new CustomException("Invalid completely or expired OTP mapping"));

        if (token.getExpiryTime().before(new Date())) {
            tokenRepository.deleteByEmail(email);
            throw new CustomException("OTP has expired.");
        }

        token.setVerified(true);
        tokenRepository.save(token);

        return Map.of("message", "OTP Verified Successfully");
    }

    // ✅ Map New Password Bounds Customization
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String newPassword = payload.get("newPassword");

        PasswordResetToken token = tokenRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("No active validation flow located"));

        if (!token.isVerified()) {
            throw new CustomException("OTP was never explicitly verified securely.");
        }

        service.resetPassword(email, newPassword);
        tokenRepository.deleteByEmail(email); // Clean tokens upon valid closures

        return Map.of("message", "Password bounded successfully completely");
    }
}