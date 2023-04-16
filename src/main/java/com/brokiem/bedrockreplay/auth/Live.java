/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.brokiem.bedrockreplay.auth.interfaces.DevicePollResult;
import com.brokiem.bedrockreplay.auth.interfaces.LiveTokenResult;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Timer;
import java.util.TimerTask;

public class Live {

    private static final String LIVE_CONNECT_URL = "https://login.live.com/oauth20_connect.srf";
    private static final String LIVE_TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private static Live instance;

    public static Live getInstance() {
        if (instance == null) {
            instance = new Live();
        }

        return instance;
    }

    public void requestLiveToken(DevicePollResult devicePollResult, LiveTokenResult callback) {
        JSONObject deviceAuth = startDeviceAuth();

        devicePollResult.onComplete(
                deviceAuth.getString("device_code"),
                deviceAuth.getString("user_code"),
                deviceAuth.getString("verification_uri"),
                deviceAuth.getIntValue("expires_in"),
                deviceAuth.getIntValue("interval")
        );

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    JSONObject result = pollDeviceAuth(deviceAuth.getString("device_code"));

                    if (result.containsKey("error")) {
                        System.out.println(result.getString("error_description"));
                        return;
                    }

                    String accessToken = result.getString("access_token");
                    String refreshToken = result.getString("refresh_token");

                    callback.onComplete(accessToken, refreshToken);
                    timer.cancel();
                } catch (Exception ignored) {
                }
            }
        }, 0, deviceAuth.getLong("interval") * 1000);
    }

    @SneakyThrows
    public JSONObject startDeviceAuth() {
        HttpClient httpClient = HttpClient.newBuilder().build();

        String requestBody = String.join("&", new String[]{
                "client_id=00000000441cc96b",
                "scope=service::user.auth.xboxlive.com::MBI_SSL",
                "response_type=device_code"
        });

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LIVE_CONNECT_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to start device auth");
        }

        return JSON.parseObject(response.body());
    }

    public JSONObject pollDeviceAuth(String deviceCode) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        String requestBody = String.join("&", new String[]{
                "client_id=00000000441cc96b",
                "grant_type=urn:ietf:params:oauth:grant-type:device_code",
                "device_code=" + deviceCode
        });

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(LIVE_TOKEN_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception(response.body());
        }

        JSONObject json = JSON.parseObject(response.body());
        if (json.containsKey("error")) {
            if (json.getString("error").equals("authorization_pending")) {
                return null;
            } else {
                throw new Exception("non-empty unknown poll error: " + json.getString("error"));
            }
        } else {
            return JSON.parseObject(response.body());
        }
    }

    @SneakyThrows
    public JSONObject refreshToken(String refreshToken) {
        HttpClient client = HttpClient.newBuilder().build();

        String requestBody = String.join("&", new String[]{
                "client_id=00000000441cc96b",
                "scope=service::user.auth.xboxlive.com::MBI_SSL",
                "grant_type=refresh_token",
                "refresh_token=" + refreshToken
        });

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(LIVE_TOKEN_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception(response.body());
        }

        return JSON.parseObject(response.body());
    }
}
