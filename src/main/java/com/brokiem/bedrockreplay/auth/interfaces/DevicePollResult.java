package com.brokiem.bedrockreplay.auth.interfaces;

public interface DevicePollResult {
    void onComplete(String deviceCode, String userCode, String verificationUrl, int expiresIn, int interval);
}
