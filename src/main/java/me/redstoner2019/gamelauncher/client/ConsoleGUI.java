package me.redstoner2019.gamelauncher.client;

import javax.swing.*;
import java.awt.*;

public class ConsoleGUI {
    private static JFrame frame;
    private static int width = 1280;
    private static int height = 720;
    public static JTextArea consoleArea = new JTextArea();
    public ConsoleGUI(){
        start();
        ConsoleGUI gui = this;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while (frame.isVisible()){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                synchronized (gui){
                    gui.notify();
                }
            }
        });
        t.start();
        synchronized (gui){
            try {
                gui.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void start() {
        /*Thread gui = new Thread(new Runnable() {
            @Override
            public void run() {
                initialize();
            }
        });
        gui.start();*/
        initialize();
    }
    private static void initialize() {
        frame = new JFrame();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setBounds(0, 0, width, height);
        frame.setTitle("OD Launcher");
        frame.setLocationRelativeTo(null);

        JPanel pan = new JPanel();
        frame.getContentPane().add(pan, BorderLayout.CENTER);
        pan.setLayout(null);

        JLabel panel = new JLabel();
        panel.setBounds(0,0,width-16,height-39);
        pan.add(panel);

        JScrollPane consolePane = new JScrollPane(consoleArea);
        consoleArea.setEditable(false);
        consolePane.setBounds(25,25,panel.getWidth() - 50,panel.getHeight() - 50 - 100);
        panel.add(consolePane);

        frame.setVisible(true);
    }
}
