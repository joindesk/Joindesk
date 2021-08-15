package com.ariseontech.joindesk.auth.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {

    @Transactional
    void deleteByLoginAndExpiryBefore(Login login, LocalDateTime now);

    PasswordResetToken findByLogin(Login login);

    PasswordResetToken findByLoginAndOtp(Login login, String otp);

}