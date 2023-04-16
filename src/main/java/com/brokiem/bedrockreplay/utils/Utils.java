/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.utils;

public class Utils {

    public static byte[] toByteArray(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (int) (value & 0xFFL);
            value >>= 8L;
        }

        return result;
    }
}
