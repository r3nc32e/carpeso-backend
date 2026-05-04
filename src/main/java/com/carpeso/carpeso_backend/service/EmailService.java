package com.carpeso.carpeso_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private void sendEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Attach logo as inline image
        try {
            org.springframework.core.io.ClassPathResource logo =
                    new org.springframework.core.io.ClassPathResource("static/logo.png");
            if (logo.exists()) {
                helper.addInline("logo", logo);
            }
        } catch (Exception e) {
            System.out.println("Logo attachment failed: " + e.getMessage());
        }

        mailSender.send(message);
    }

    private String getEmailTemplate(String title, String greeting,
                                    String body, String footer) {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 20px;">
                    <table width="600" cellpadding="0" cellspacing="0"
                        style="background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                        <!-- Header -->
                        <tr><td style="background:#DC2626;padding:30px;text-align:center;">
                            <img src="cid:logo"
                                style="width:70px;height:70px;border-radius:50%;border:3px solid white;margin-bottom:12px;"
                                onerror="this.style.display='none'" />
                            <h1 style="color:white;margin:0;font-size:28px;">Carpeso</h1>
                            <p style="color:#fca5a5;margin:8px 0 0;font-style:italic;">
                                "Drive the deal. Own the wheel."
                            </p>
                        </td></tr>
                        <!-- Title -->
                        <tr><td style="background:#FEF2F2;padding:20px;text-align:center;">
                            <h2 style="color:#DC2626;margin:0;">""" + title + """
                            </h2>
                        </td></tr>
                        <!-- Body -->
                        <tr><td style="padding:30px;">
                            <p style="color:#374151;font-size:16px;">""" + greeting + """
                            </p>
                            """ + body + """
                            <p style="color:#6B7280;font-size:14px;margin-top:24px;">""" + footer + """
                            </p>
                        </td></tr>
                        <!-- Footer -->
                        <tr><td style="background:#DC2626;padding:20px;text-align:center;">
                            <p style="color:#fca5a5;margin:0;font-size:12px;">
                                © 2026 Carpeso — All Rights Reserved
                            </p>
                        </td></tr>
                    </table>
                </td></tr>
            </table>
        </body>
        </html>
        """;
    }

    public void sendRegistrationOtp(String email, String firstName, String otp) throws Exception {
        String body = """
            <div style="background:#FEF2F2;border:2px solid #DC2626;border-radius:12px;
                padding:20px;text-align:center;margin:20px 0;">
                <p style="color:#6B7280;margin:0 0 8px;font-size:14px;">
                    Your Verification Code
                </p>
                <h1 style="color:#DC2626;font-size:48px;letter-spacing:12px;margin:0;">
                    """ + otp + """
                </h1>
                <p style="color:#6B7280;margin:8px 0 0;font-size:12px;">
                    Expires in 10 minutes
                </p>
            </div>
            <p style="color:#374151;">
                Use this code to verify your email and complete your registration.
                Do not share this code with anyone.
            </p>
            """;

        String html = getEmailTemplate(
                "Email Verification",
                "Hello, " + firstName + "! Welcome to Carpeso!",
                body,
                "If you did not register at Carpeso, please ignore this email."
        );

        sendEmail(email, "🚗 Carpeso — Verify Your Email", html);
    }

    public void sendLoginOtp(String email, String firstName, String otp) throws Exception {
        String body = """
            <div style="background:#FEF2F2;border:2px solid #DC2626;border-radius:12px;
                padding:20px;text-align:center;margin:20px 0;">
                <p style="color:#6B7280;margin:0 0 8px;font-size:14px;">
                    Your Login OTP Code
                </p>
                <h1 style="color:#DC2626;font-size:48px;letter-spacing:12px;margin:0;">
                    """ + otp + """
                </h1>
                <p style="color:#6B7280;margin:8px 0 0;font-size:12px;">
                    Expires in 5 minutes
                </p>
            </div>
            <p style="color:#374151;">
                Someone is attempting to login to your Carpeso account.
                If this was you, use the code above. If not, please secure your account immediately.
            </p>
            """;

        String html = getEmailTemplate(
                "Login Verification",
                "Hello, " + firstName + "!",
                body,
                "If you did not attempt to login, please change your password immediately."
        );

        sendEmail(email, "🚗 Carpeso — Login OTP", html);
    }

    public void sendForgotPasswordOtp(String email, String firstName, String otp) throws Exception {
        String body = """
            <div style="background:#FEF2F2;border:2px solid #DC2626;border-radius:12px;
                padding:20px;text-align:center;margin:20px 0;">
                <p style="color:#6B7280;margin:0 0 8px;font-size:14px;">
                    Password Reset Code
                </p>
                <h1 style="color:#DC2626;font-size:48px;letter-spacing:12px;margin:0;">
                    """ + otp + """
                </h1>
                <p style="color:#6B7280;margin:8px 0 0;font-size:12px;">
                    Expires in 10 minutes
                </p>
            </div>
            <p style="color:#374151;">
                Use this code to reset your password.
                This code is valid for 10 minutes only.
            </p>
            """;

        String html = getEmailTemplate(
                "Password Reset",
                "Hello, " + firstName + "!",
                body,
                "If you did not request a password reset, please ignore this email."
        );

        sendEmail(email, "🚗 Carpeso — Password Reset OTP", html);
    }

    public void sendPasswordResetConfirmation(String email, String firstName) throws Exception {
        String body = """
            <div style="background:#F0FDF4;border:2px solid #16A34A;border-radius:12px;
                padding:20px;text-align:center;margin:20px 0;">
                <h2 style="color:#16A34A;margin:0;">✅ Password Changed Successfully!</h2>
            </div>
            <p style="color:#374151;">
                Your Carpeso account password has been successfully changed.
                You can now login with your new password.
            </p>
            """;

        String html = getEmailTemplate(
                "Password Changed",
                "Hello, " + firstName + "!",
                body,
                "If you did not change your password, please contact us immediately."
        );

        sendEmail(email, "🚗 Carpeso — Password Changed", html);
    }

    public void sendOrderStatusUpdate(String email, String firstName,
                                      String vehicleName, String status, String notes) throws Exception {
        String statusColor = switch (status) {
            case "CONFIRMED" -> "#2563EB";
            case "PREPARING" -> "#7C3AED";
            case "READY" -> "#4F46E5";
            case "OUT_FOR_DELIVERY" -> "#D97706";
            case "DELIVERED" -> "#16A34A";
            case "COMPLETED" -> "#15803D";
            case "CANCELLED" -> "#DC2626";
            default -> "#6B7280";
        };

        String statusLabel = status.replace("_", " ");

        String body = """
            <div style="background:#F9FAFB;border-radius:12px;padding:20px;margin:20px 0;">
                <p style="color:#6B7280;margin:0 0 8px;font-size:14px;">Vehicle</p>
                <h3 style="color:#111827;margin:0 0 16px;">""" + vehicleName + """
                </h3>
                <p style="color:#6B7280;margin:0 0 8px;font-size:14px;">New Status</p>
                <div style="display:inline-block;background:""" + statusColor + """
                    ;color:white;padding:8px 20px;border-radius:50px;font-weight:bold;">
                    """ + statusLabel + """
                </div>
                """ + (notes != null && !notes.isEmpty() ?
                "<p style='color:#6B7280;margin-top:16px;font-size:14px;'>" +
                "Admin Notes: " + notes + "</p>" : "") + """
            </div>
            <p style="color:#374151;">
                Your order status has been updated. Login to Carpeso to view more details.
            </p>
            """;

        String html = getEmailTemplate(
                "Order Status Update",
                "Hello, " + firstName + "! Your order has been updated.",
                body,
                "Thank you for choosing Carpeso!"
        );

        sendEmail(email, "🚗 Carpeso — Order Update: " + statusLabel, html);
    }

    public void sendReservationConfirmation(String email, String firstName,
                                            String vehicleName, String expiresAt) throws Exception {
        String body = """
            <div style="background:#F0FDF4;border:2px solid #16A34A;border-radius:12px;
                padding:20px;margin:20px 0;">
                <h3 style="color:#16A34A;margin:0 0 8px;">✅ Reservation Confirmed!</h3>
                <p style="color:#374151;margin:0;font-size:16px;font-weight:bold;">
                    """ + vehicleName + """
                </p>
            </div>
            <p style="color:#374151;">
                Your reservation has been successfully submitted.
                Our admin will review and confirm your booking shortly.
            </p>
            <div style="background:#FEF3C7;border-radius:12px;padding:16px;margin:16px 0;">
                <p style="color:#92400E;margin:0;font-size:14px;">
                    ⚠️ Important: Your reservation expires on <strong>""" + expiresAt + """
                    </strong>. Please complete the transaction before it expires.
                </p>
            </div>
            """;

        String html = getEmailTemplate(
                "Reservation Submitted",
                "Hello, " + firstName + "! Thank you for choosing Carpeso!",
                body,
                "If you have any questions, please contact us."
        );

        sendEmail(email, "🚗 Carpeso — Reservation Confirmed", html);
    }

    public void sendAdminCreated(String email, String firstName,
                                 String password, String role) throws Exception {
        String body = """
            <div style="background:#F9FAFB;border-radius:12px;padding:20px;margin:20px 0;">
                <p style="color:#6B7280;margin:0 0 4px;font-size:14px;">Email</p>
                <p style="color:#111827;font-weight:bold;margin:0 0 16px;">""" + email + """
                </p>
                <p style="color:#6B7280;margin:0 0 4px;font-size:14px;">Temporary Password</p>
                <p style="color:#DC2626;font-weight:bold;font-size:20px;
                    letter-spacing:4px;margin:0;">""" + password + """
                </p>
            </div>
            <div style="background:#FEF2F2;border-radius:12px;padding:16px;margin:16px 0;">
                <p style="color:#991B1B;margin:0;font-size:14px;">
                    🔒 Please change your password immediately after first login.
                </p>
            </div>
            """;

        String html = getEmailTemplate(
                "Admin Account Created",
                "Hello, " + firstName + "! Your Carpeso admin account has been created.",
                body,
                "This is a system-generated email. Do not reply."
        );

        sendEmail(email, "🚗 Carpeso — Admin Account Created", html);
    }

    public void sendWarrantyClaimUpdate(String email, String firstName,
                                        String status, String adminResponse) throws Exception {
        String title = status.equals("APPROVED")
                ? "✅ Warranty Claim Approved"
                : "❌ Warranty Claim Update";
        String body = "<div style='background:#f9f9f9;padding:16px;border-radius:12px;'>"
                + "<p style='white-space:pre-wrap;color:#374151;font-size:14px;'>"
                + (adminResponse != null ? adminResponse.replace("\n", "<br>") : "Your claim has been reviewed.")
                + "</p></div>";
        String html = getEmailTemplate(title,
                "Dear " + firstName + ",",
                body,
                "Thank you for your patience. — Carpeso Support Team");
        sendEmail(email, "Carpeso — Warranty Claim " + status, html);
    }
    public void sendAccountSuspension(String email, String firstName,
                                      String reason, String duration) throws Exception {
        String body = "<div style='background:#FEF2F2;border:1px solid #FECACA;padding:16px;border-radius:12px;'>"
                + "<p style='color:#374151;font-size:14px;line-height:1.6;'>"
                + "We regret to inform you that your Carpeso account has been <strong>suspended</strong>.<br><br>"
                + "<strong>Reason:</strong> " + reason + "<br>"
                + "<strong>Duration:</strong> " + duration + "<br><br>"
                + "Your actions have violated our Terms and Conditions. "
                + "If you believe this is a mistake, please contact our support team "
                + "at <a href='mailto:support@carpeso.com'>support@carpeso.com</a>.<br><br>"
                + "We apologize for any inconvenience caused."
                + "</p></div>";
        String html = getEmailTemplate(
                "Account Suspended",
                "Dear " + firstName + ",",
                body,
                "— Carpeso Support Team");
        sendEmail(email, "Carpeso — Account Suspension Notice", html);
    }
    public void sendWarningNotification(String email, String firstName,
                                        String reason, int warningCount) throws Exception {
        String body = "<div style='background:#FFF7ED;border:1px solid #FED7AA;"
                + "padding:16px;border-radius:12px;'>"
                + "<p style='color:#374151;font-size:14px;line-height:1.6;'>"
                + "You have received <strong>Warning #" + warningCount + "</strong> "
                + "on your Carpeso account.<br><br>"
                + "<strong>Reason:</strong> " + reason + "<br><br>"
                + "⚠️ Please note that <strong>3 warnings</strong> will result in "
                + "automatic account suspension.<br><br>"
                + "Please review our <a href='http://localhost:5173/terms'>Terms and Conditions</a> "
                + "to avoid further violations."
                + "</p></div>";
        String html = getEmailTemplate(
                "⚠️ Warning Issued — Carpeso",
                "Dear " + firstName + ",",
                body,
                "If you believe this is a mistake, contact support@carpeso.com");
        sendEmail(email, "Carpeso — Warning Notice #" + warningCount, html);
    }

    public void sendAccountReinstatement(String email,
                                         String firstName) throws Exception {
        String body = "<div style='background:#F0FDF4;border:1px solid #BBF7D0;"
                + "padding:16px;border-radius:12px;'>"
                + "<p style='color:#374151;font-size:14px;line-height:1.6;'>"
                + "Great news! Your Carpeso account has been <strong>reinstated</strong>.<br><br>"
                + "You can now log in and resume using our services. "
                + "Please ensure that you comply with our Terms and Conditions "
                + "to avoid future suspensions.<br><br>"
                + "Welcome back to Carpeso!"
                + "</p></div>";
        String html = getEmailTemplate(
                "✅ Account Reinstated",
                "Dear " + firstName + ",",
                body,
                "Thank you for your patience. — Carpeso Support Team");
        sendEmail(email, "Carpeso — Account Reinstated", html);
    }
}