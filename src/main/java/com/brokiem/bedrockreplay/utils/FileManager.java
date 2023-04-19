/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.utils;

import com.brokiem.bedrockreplay.BedrockReplay;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class FileManager {

    public static InputStream getFileResourceAsInputStream(String name) {
        return BedrockReplay.class.getClassLoader().getResourceAsStream(name);
    }

    public static BufferedImage getFileResourceAsImage(String name) {
        try {
            URL resource = BedrockReplay.class.getClassLoader().getResource(name);
            assert resource != null;
            return ImageIO.read(resource);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static String getFileResourceAsString(String name) {
        return getFileContents(getFileResourceAsInputStream(name));
    }

    public static void writeToFile(String name, String content) {
        try {
            FileWriter fileWriter = new FileWriter(name);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFileContents(String name) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(name));
            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String fileContents = sb.toString();
            reader.close();

            return fileContents;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getFileContents(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            byteArrayOutputStream.close();
            inputStream.close();

            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String decompressGZIP(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPInputStream gZIPInputStream = new GZIPInputStream(inputStream);
        byte[] buffer = new byte[1024];

        int length;
        while ((length = gZIPInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }

        byteArrayOutputStream.close();
        inputStream.close();
        gZIPInputStream.close();
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }
}
