package me.redstoner2019.serverhandling;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class LocalNetworkScanner {
    private static final int PORT = 8008;
    private static final int TIMEOUT = 100;
    private static final Object LOCK = new Object();

    public static void scan(final List<String> serverList, final int port) throws IOException {
        //List<String> serverList = new ArrayList<>();
        serverList.clear();
        System.out.println(InetAddress.getLocalHost().getHostAddress().replace(".", "-"));
        final String[] ipArr = InetAddress.getLocalHost().getHostAddress().replace(".", "-").split("-");

        final int[] ipsScanned = {0};
        final int[] threadsStarted = {0};

        for (int i = 0; i <= 255; i++) {
                final int finalI = i;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        threadsStarted[0]++;
                        String ip = ipArr[0] + "." + ipArr[1] + "." + ipArr[2] + "." + finalI;
                        //System.out.println("Scanning " + ip);
                        if (isReachable(ip) && isOpen(ip, port)) {
                            serverList.add(ip);
                        }
                        ipsScanned[0]++;
                    /*if (ipsScanned[0] >= threadsStarted[0]) {
                        synchronized (LOCK) {
                            LOCK.notify();
                        }
                    }*/
                    }
                });
                t.start();

        }
        /*synchronized (LOCK) {
            try {
                LOCK.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        ConnectGUI.serverList = List.copyOf(serverList);*/
        System.out.println(serverList.size());
        //return serverList;
    }
    private static boolean isReachable(String ip) {
        try {
            return InetAddress.getByName(ip).isReachable(100);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isOpen(String ip, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e){
                // Ignore Exception
            }
        }
    }
}