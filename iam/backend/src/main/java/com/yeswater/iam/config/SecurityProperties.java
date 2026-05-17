package com.yeswater.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iam.security")
public class SecurityProperties {

    private long mfaOtpTtlSeconds;

    private long oidcAuthCodeTtlSeconds;

    private long clientSecretGraceSeconds;

    public long getMfaOtpTtlSeconds() {
        return mfaOtpTtlSeconds;
    }

    public void setMfaOtpTtlSeconds(long mfaOtpTtlSeconds) {
        this.mfaOtpTtlSeconds = mfaOtpTtlSeconds;
    }

    public long getOidcAuthCodeTtlSeconds() {
        return oidcAuthCodeTtlSeconds;
    }

    public void setOidcAuthCodeTtlSeconds(long oidcAuthCodeTtlSeconds) {
        this.oidcAuthCodeTtlSeconds = oidcAuthCodeTtlSeconds;
    }

    public long getClientSecretGraceSeconds() {
        return clientSecretGraceSeconds;
    }

    public void setClientSecretGraceSeconds(long clientSecretGraceSeconds) {
        this.clientSecretGraceSeconds = clientSecretGraceSeconds;
    }
}
