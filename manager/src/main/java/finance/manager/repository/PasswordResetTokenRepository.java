package finance.manager.repository;

import finance.manager.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByEmailAndOtp(String email, String otp);
    
    Optional<PasswordResetToken> findByEmail(String email);
    
    void deleteByEmail(String email);
}
