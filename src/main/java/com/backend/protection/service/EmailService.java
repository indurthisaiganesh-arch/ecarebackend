package com.backend.protection.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * ─── EmailService ─────────────────────────────────────────────────────────────
 * Sends transactional HTML emails via SendGrid HTTP API.
 *
 * All methods are @Async — email delivery never blocks the calling thread.
 * Configure SendGrid API key in application.properties or Render Environment.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${app.email.from:admin.caredigital@gmail.com}")
    private String fromEmail;

    @Value("${app.email.from-name:E-Care Digital}")
    private String fromName;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    // ── Core Send ─────────────────────────────────────────────────────────────

    /**
     * Sends an HTML email asynchronously via SendGrid HTTP API.
     *
     * @param to       Recipient email address
     * @param subject  Email subject line
     * @param htmlBody HTML body content
     */
    @Async
    public void sendHtml(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.info("[EMAIL DISABLED] Would send '{}' to {}", subject, to);
            return;
        }
        try {
            Email from = new Email(fromEmail, fromName);
            Email recipient = new Email(to);
            
            // Explicitly set to text/html to render your templates correctly
            Content content = new Content("text/html", htmlBody); 
            Mail mail = new Mail(from, subject, recipient, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("[EMAIL ERROR] SendGrid rejected '{}' to {}: {}", subject, to, response.getBody());
            } else {
                log.info("[EMAIL SENT] '{}' → {}", subject, to);
            }
        } catch (IOException e) {
            log.error("[EMAIL ERROR] Failed to send '{}' to {}: {}", subject, to, e.getMessage());
        }
    }



    // ── OTP Email ─────────────────────────────────────────────────────────────

    /**
     * Sends a 6-digit OTP code to the user's email.
     *
     * @param recipientEmail Recipient email address
     * @param recipientName  Display name shown in the email
     * @param otpCode        The 6-digit OTP code
     * @param purpose        LOGIN | REGISTER | FORGOT_PASSWORD
     */
    @Async
    public void sendOtpEmail(String recipientEmail, String recipientName, String otpCode, String purpose) {
        String purposeLabel = switch (purpose.toUpperCase()) {
            case "REGISTER" -> "email verification";
            case "FORGOT_PASSWORD" -> "password reset";
            default -> "login verification";
        };

        String subject = "E-Care Digital — Your Verification Code: " + otpCode;
        String html = buildOtpEmailHtml(recipientName, otpCode, purposeLabel);
        sendHtml(recipientEmail, subject, html);
        log.info("[OTP EMAIL] Sent OTP {} to {} for purpose {}", otpCode, recipientEmail, purpose);
    }

    // ── Welcome Email ─────────────────────────────────────────────────────────

    /**
     * Sends a welcome email to a newly created user.
     *
     * @param recipientEmail Recipient email
     * @param recipientName  Full name
     * @param role           Assigned system role
     */
    @Async
    public void sendWelcomeEmail(String recipientEmail, String recipientName, String role) {
        String subject = "Welcome to E-Care Digital — Your Account is Ready";
        String html = buildWelcomeEmailHtml(recipientName, role, recipientEmail);
        sendHtml(recipientEmail, subject, html);
    }

    // ── Account Approval Email ────────────────────────────────────────────────

    /**
     * Notifies a self-registered user that their account was approved.
     */
    @Async
    public void sendApprovalEmail(String recipientEmail, String recipientName) {
        String subject = "Your E-Care Digital Account Has Been Approved ✓";
        String html = buildApprovalEmailHtml(recipientName);
        sendHtml(recipientEmail, subject, html);
    }

    // ── Account Rejection Email ───────────────────────────────────────────────

    /**
     * Notifies a self-registered user that their account was rejected.
     */
    @Async
    public void sendRejectionEmail(String recipientEmail, String recipientName, String reason) {
        String subject = "E-Care Digital — Account Registration Update";
        String html = buildRejectionEmailHtml(recipientName,
                reason != null ? reason : "Your registration did not meet the current access requirements.");
        sendHtml(recipientEmail, subject, html);
    }

    // ── Appointment Confirmation Email ────────────────────────────────────────

    /**
     * Sends appointment confirmation to a patient.
     */
    @Async
    public void sendAppointmentConfirmationEmail(String recipientEmail, String recipientName,
                                                  String doctorName, String appointmentDate,
                                                  String department, String notes) {
        String subject = "Appointment Confirmed — E-Care Digital";
        String html = buildAppointmentConfirmationHtml(recipientName, doctorName, appointmentDate, department, notes);
        sendHtml(recipientEmail, subject, html);
    }

    // ── Appointment Cancellation Email ────────────────────────────────────────

    /**
     * Sends appointment cancellation to a patient.
     */
    @Async
    public void sendAppointmentCancellationEmail(String recipientEmail, String recipientName,
                                                  String doctorName, String appointmentDate) {
        String subject = "Appointment Cancelled — E-Care Digital";
        String html = buildAppointmentCancellationHtml(recipientName, doctorName, appointmentDate);
        sendHtml(recipientEmail, subject, html);
    }

    // ── Password Reset Email ──────────────────────────────────────────────────

    /**
     * Sends password reset instructions.
     */
    @Async
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
        String subject = "Reset Your E-Care Digital Password";
        String html = buildPasswordResetHtml(recipientName, resetToken);
        sendHtml(recipientEmail, subject, html);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HTML EMAIL TEMPLATES
    // ═════════════════════════════════════════════════════════════════════════

    private String buildOtpEmailHtml(String name, String otp, String purposeLabel) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(6,182,212,0.2);overflow:hidden;">
                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#06b6d4,#3b82f6);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;letter-spacing:-0.02em;">E-CARE DIGITAL</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">Secure Healthcare Platform</div>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:40px;">
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 20px 0;">Hello %s,</p>
                        <p style="color:#f8fafc;font-size:15px;margin:0 0 32px 0;">
                          Your <strong style="color:#06b6d4;">%s</strong> code for E-Care Digital is:
                        </p>
                        <!-- OTP Box -->
                        <div style="background:#090d16;border:2px solid #06b6d4;border-radius:12px;padding:28px;text-align:center;margin-bottom:32px;box-shadow:0 0 32px rgba(6,182,212,0.2);">
                          <div style="font-family:monospace;font-size:48px;font-weight:800;letter-spacing:0.3em;color:#ffffff;text-shadow:0 0 20px rgba(6,182,212,0.5);">%s</div>
                          <div style="color:#64748b;font-size:12px;margin-top:8px;">Valid for 10 minutes · Do not share this code</div>
                        </div>
                        <p style="color:#64748b;font-size:13px;margin:0 0 8px 0;">
                          If you did not request this code, please ignore this email. Your account remains secure.
                        </p>
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">
                          © 2026 E-Care Digital · AES-256-GCM Encrypted · Zero-Trust Security
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, purposeLabel, otp);
    }

    private String buildWelcomeEmailHtml(String name, String role, String email) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(6,182,212,0.2);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#06b6d4,#3b82f6);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;">E-CARE DIGITAL</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">Welcome to the Platform</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="color:#f8fafc;font-size:22px;margin:0 0 16px 0;">Welcome, %s! 👋</h2>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 20px 0;">
                          Your E-Care Digital account has been created with the role:
                          <span style="background:rgba(6,182,212,0.15);color:#06b6d4;padding:2px 10px;border-radius:20px;font-weight:700;">%s</span>
                        </p>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 8px 0;">Login with: <strong style="color:#f8fafc;">%s</strong></p>
                        <p style="color:#94a3b8;font-size:13px;margin:0 0 32px 0;">
                          Your account is active and ready to use. Login with your credentials and complete 2-factor verification.
                        </p>
                        <a href="http://localhost:5173/login"
                           style="background:linear-gradient(135deg,#06b6d4,#3b82f6);color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:700;display:inline-block;">
                          Sign In to E-Care Digital →
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">© 2026 E-Care Digital · AES-256-GCM Encrypted</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, role, email);
    }

    private String buildApprovalEmailHtml(String name) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(16,185,129,0.25);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#10b981,#06b6d4);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;">E-CARE DIGITAL</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">Account Approved ✓</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="color:#f8fafc;font-size:22px;margin:0 0 16px 0;">Great news, %s!</h2>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">
                          Your registration has been <strong style="color:#34d399;">reviewed and approved</strong> by an administrator.
                          You can now sign in to the E-Care Digital platform.
                        </p>
                        <a href="http://localhost:5173/login"
                           style="background:linear-gradient(135deg,#10b981,#06b6d4);color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:700;display:inline-block;">
                          Sign In Now →
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">© 2026 E-Care Digital</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name);
    }

    private String buildRejectionEmailHtml(String name, String reason) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(239,68,68,0.2);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#ef4444,#7c3aed);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;">E-CARE DIGITAL</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">Account Registration Update</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="color:#f8fafc;font-size:22px;margin:0 0 16px 0;">Hello %s,</h2>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 16px 0;">
                          We were unable to approve your registration request at this time.
                        </p>
                        <div style="background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);border-radius:8px;padding:16px;margin-bottom:24px;">
                          <p style="color:#f87171;font-size:13px;margin:0;"><strong>Reason:</strong> %s</p>
                        </div>
                        <p style="color:#64748b;font-size:13px;margin:0;">
                          For further assistance, contact <a href="mailto:admin.caredigital@gmail.com" style="color:#06b6d4;">admin.caredigital@gmail.com</a>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">© 2026 E-Care Digital</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, reason);
    }

    private String buildAppointmentConfirmationHtml(String patientName, String doctorName,
                                                     String date, String department, String notes) {
        String notesRow = (notes != null && !notes.isBlank())
                ? "<p style=\"color:#94a3b8;font-size:14px;margin:8px 0 0 0;\"><strong style=\"color:#f8fafc;\">Notes:</strong> " + notes + "</p>"
                : "";
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(6,182,212,0.2);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#06b6d4,#3b82f6);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;">Appointment Confirmed</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">E-Care Digital</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">Hello <strong style="color:#f8fafc;">%s</strong>,</p>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">Your appointment has been confirmed:</p>
                        <div style="background:#090d16;border:1px solid rgba(6,182,212,0.2);border-radius:12px;padding:24px;margin-bottom:24px;">
                          <p style="color:#94a3b8;font-size:14px;margin:0 0 8px 0;"><strong style="color:#f8fafc;">Doctor:</strong> %s</p>
                          <p style="color:#94a3b8;font-size:14px;margin:0 0 8px 0;"><strong style="color:#f8fafc;">Department:</strong> %s</p>
                          <p style="color:#94a3b8;font-size:14px;margin:0;"><strong style="color:#f8fafc;">Date/Time:</strong> %s</p>
                          %s
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">© 2026 E-Care Digital</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(patientName, doctorName, department, date, notesRow);
    }

    private String buildAppointmentCancellationHtml(String patientName, String doctorName, String date) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(239,68,68,0.3);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#ef4444,#dc2626);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;">Appointment Cancelled</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">E-Care Digital</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">Hello <strong style="color:#f8fafc;">%s</strong>,</p>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">Your appointment has been cancelled:</p>
                        <div style="background:#090d16;border:1px solid rgba(239,68,68,0.2);border-radius:12px;padding:24px;margin-bottom:24px;">
                          <p style="color:#94a3b8;font-size:14px;margin:0 0 8px 0;"><strong style="color:#f8fafc;">Doctor:</strong> %s</p>
                          <p style="color:#94a3b8;font-size:14px;margin:0;"><strong style="color:#f8fafc;">Scheduled Date:</strong> %s</p>
                        </div>
                        <p style="color:#64748b;font-size:13px;margin:0;">
                          You may log in to your patient portal at any time to book another appointment.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">© 2026 E-Care Digital</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(patientName, doctorName, date);
    }

    private String buildPasswordResetHtml(String name, String resetToken) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#090d16;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#090d16;padding:40px 20px;">
                <tr><td align="center">
                  <table width="560" style="background:#0f172a;border-radius:16px;border:1px solid rgba(6,182,212,0.2);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#06b6d4,#3b82f6);padding:32px 40px;text-align:center;">
                        <div style="font-size:28px;font-weight:800;color:#fff;">Password Reset Request</div>
                        <div style="color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px;">E-Care Digital Security</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">Hello <strong style="color:#f8fafc;">%s</strong>,</p>
                        <p style="color:#94a3b8;font-size:14px;margin:0 0 24px 0;">We received a request to reset your password. Use the verification token below to proceed:</p>
                        <div style="background:#090d16;border:1px solid rgba(6,182,212,0.2);border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;">
                          <div style="font-size:24px;font-weight:700;letter-spacing:4px;color:#06b6d4;">%s</div>
                        </div>
                        <p style="color:#64748b;font-size:12px;margin:0;">
                          If you did not request this, please contact support immediately or ignore this email.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#090d16;padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
                        <p style="color:#475569;font-size:11px;margin:0;">© 2026 E-Care Digital</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, resetToken);
    }
}
