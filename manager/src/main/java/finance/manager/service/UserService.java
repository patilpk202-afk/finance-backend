package finance.manager.service;

import finance.manager.exception.CustomException;
import finance.manager.model.User;
import finance.manager.repository.UserRepository;
import finance.manager.repository.TransactionRepository;
import finance.manager.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ Register
    public User register(User user) {
        if (repository.findByEmail(user.getEmail()) != null) {
            throw new CustomException("User already registered with this email");
        }
        // Enforce cryptographic bounds actively on all new registers
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }

    public User findByEmail(String email) {
        return repository.findByEmail(email);
    }

    // ✅ Login
    public String login(String email, String password) {

        User user = repository.findByEmail(email);

        if (user != null) {
            // Fallback logically: Test plain text first (backwards compatibility) then test secure cryptographic boundary hashes
            if (user.getPassword().equals(password) || passwordEncoder.matches(password, user.getPassword())) {
                return jwtUtil.generateToken(user.getId(), email, user.getName());
            }
        }

        throw new CustomException("Invalid email or password");
    }

    // ✅ Reset Password bounds logic
    public void resetPassword(String email, String newPassword) {
        User user = repository.findByEmail(email);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            repository.save(user);
        } else {
            throw new CustomException("User does not exist");
        }
    }

    // ✅ Delete Account & Data
    public void deleteAccount(String userId) {
        transactionRepository.deleteByUserId(userId);
        repository.deleteById(userId);
    }

    // ✅ Map Custom Active Profiles extracting Base64 images directly over standard User mappings
    public User getUserProfile(String userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new CustomException("User profile safely mapped out of bounds"));
    }

    // ✅ Inject explicit Base64 blobs mapped back into Mongo dynamically cleanly restricting filesystems
    public void updateProfileImage(String userId, String base64Image) {
        User user = getUserProfile(userId);
        user.setProfileImage(base64Image);
        repository.save(user);
    }
}