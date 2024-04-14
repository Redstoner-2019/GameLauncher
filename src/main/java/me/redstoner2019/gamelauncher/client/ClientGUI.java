package me.redstoner2019.gamelauncher.client;

import javax.swing.*;
import java.awt.*;

public class ClientGUI<d> {

    private JFrame frame;
    private int width = 1920;
    private int height = 1080;

    public ClientGUI() {
        initialize();
    }
    private void initialize() {
        frame = new JFrame();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, width, height);
        frame.setTitle("me.redstoner2019.gamelauncher.client");

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(null);
    }
}
