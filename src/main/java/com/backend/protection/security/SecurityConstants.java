package com.backend.protection.security;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MINUTES = 15;
    public static final int OTP_LENGTH = 6;
    public static final long OTP_EXPIRY_MINUTES = 10;
    public static final int MAX_OTP_ATTEMPTS = 3;
    public static final String OTP_PURPOSE_LOGIN = "LOGIN";
    public static final String OTP_PURPOSE_REGISTER = "REGISTER";
    public static final String OTP_PURPOSE_RESET = "PASSWORD_RESET";
}
