/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.output;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class OutputWindow {

    @Getter
    private static JTextArea textArea;

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("BedrockReplay Output");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);

        // Set the look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JPanel panel = new JPanel();
        JTextArea textArea = new JTextArea(15, 80);
        Font currentFont = textArea.getFont();
        Font newFont = new Font(currentFont.getFamily(), currentFont.getStyle(), 16);
        textArea.setFont(newFont);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                int startOffset = textArea.viewToModel2D(new Point(x, y));
                String text = textArea.getText();
                int searchHttp;
                int wordEndIndex;
                String[] words = text.split("\\s");

                for (String word : words) {
                    if (word.startsWith("https://") || word.startsWith("http://")) {
                        searchHttp = text.indexOf(word);
                        wordEndIndex = searchHttp + word.length();
                        if (startOffset >= searchHttp && startOffset <= wordEndIndex) {
                            try {
                                textArea.select(searchHttp, wordEndIndex);
                                Desktop.getDesktop().browse(new URI(word));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });
        OutputWindow.textArea = textArea;
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane);
        frame.add(panel, BorderLayout.CENTER);

        // Create the close button and add it to the frame
        JButton closeButton = new JButton("Exit");
        closeButton.addActionListener(e -> System.exit(0));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        final Point[] mouseOffset = new Point[1];
        // Add a mouse listener to the panel to make it draggable
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseOffset[0] = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseOffset[0] = null;
            }
        });
        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentLocation = frame.getLocation();
                frame.setLocation(currentLocation.x + e.getX() - mouseOffset[0].x, currentLocation.y + e.getY() - mouseOffset[0].y);
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void print(String text) {
        textArea.append("> " + text + "\n");
        System.out.println(text);
    }
}