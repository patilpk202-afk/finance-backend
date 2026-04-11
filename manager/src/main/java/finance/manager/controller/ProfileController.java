package finance.manager.controller;

import finance.manager.model.User;
import finance.manager.service.UserService;
import finance.manager.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserService service;

    // ✅ Map Custom Image Array bounding cleanly as Base64 strings saving into MongoDB seamlessly
    @PostMapping("/upload-profile-pic")
    public Map<String, String> uploadProfilePic(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) throw new CustomException("Unauthorized Context Mapping");

        try {
            // Explicit constraints natively verifying MIME structure and limits gracefully
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new CustomException("Only active image layouts are securely accepted");
            }
            if (file.getSize() > 2 * 1024 * 1024) { // 2MB restriction natively
                throw new CustomException("Maximum upload structure is bounded strictly at 2MB");
            }

            // Encapsulate dynamic arrays straight into Base64 format mapping Data URIs perfectly out of the box dynamically!
            String base64Image = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
            
            service.updateProfileImage(userId, base64Image);
            
            return Map.of("message", "Profile Avatar captured mapped flawlessly", "profileImage", base64Image);
            
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CustomException("Failed to encode or validate Avatar binaries natively");
        }
    }

    // ✅ Resolve Profile metadata directly bypassing passwords dynamically!
    @GetMapping("/profile")
    public Map<String, String> getProfile(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        User user = service.getUserProfile(userId);
        
        return Map.of(
            "name", user.getName() != null ? user.getName() : "User",
            "email", user.getEmail() != null ? user.getEmail() : "",
            "profileImage", user.getProfileImage() != null ? user.getProfileImage() : ""
        );
    }
}
