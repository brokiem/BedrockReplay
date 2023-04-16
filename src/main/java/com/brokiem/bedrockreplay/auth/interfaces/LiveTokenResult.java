package com.brokiem.bedrockreplay.auth.interfaces;

public interface LiveTokenResult {
    void onComplete(String accessToken, String refreshToken);
}
