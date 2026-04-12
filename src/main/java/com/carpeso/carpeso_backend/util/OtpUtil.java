package com.carpeso.carpeso_backend.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Component
public class OtpUtil {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public LocalDateTime getOtpExpiry() {
        return LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
    }

    public boolean isOtpValid(String storedOtp, String inputOtp,
                              LocalDateTime expiry) {
        if (storedOtp == null || inputOtp == null || expiry == null) {
            return false;
        }
        return storedOtp.equals(inputOtp) &&
                LocalDateTime.now().isBefore(expiry);
    }
}