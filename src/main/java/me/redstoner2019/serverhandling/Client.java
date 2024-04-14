package me.redstoner2019.serverhandling;


import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Client {
    public static Socket socket = null;
    public static ObjectOutputStream out;
    public static ObjectInputStream in;
    public static PacketListener listener;
    public static boolean isConnected = false;
    public static ConnectionFailedEvent connectionFailEvent = null;
    public static ConnectionSuccessEvent connectionSuccessEvent = null;
    public static ConnectionLostEvent connectionLostEvent = null;
    public static int packetsRecieved = 0;
    public static int packetsSent = 0;
    private static List<Object> toSend = new ArrayList<>();

    public static void setConnectionLostEvent(ConnectionLostEvent connectionLostEvent) {
        Client.connectionLostEvent = connectionLostEvent;
    }

    public static boolean isConnected(){
        return isConnected;
    }
    public static void connect(String address, int port){
        try {
            socket = new Socket(address,port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            isConnected = true;
            if(connectionSuccessEvent != null) {
                connectionSuccessEvent.onConnectionSuccess();
                System.out.println("connectionSuccessEvent");
            }
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (!socket.isClosed()){
                        if(in == null) continue;
                        if(listener == null){
                            continue;
                        }
                        try {
                            Object o = in.readObject();
                            listener.packetRecievedEvent(o);
                            packetsRecieved++;
                        } catch (ClassNotFoundException ignored){
                            System.err.println("Class not found");
                            ignored.printStackTrace();
                        } catch (ClassCastException ignored){
                            System.err.println("Couldnt cast class");
                            ignored.printStackTrace();
                        } catch (StreamCorruptedException ignored){
                            System.err.println("Stream corrupted");
                            ignored.printStackTrace();
                            try {
                                in.reset();
                            } catch (IOException e) {
                                if(connectionLostEvent != null) connectionLostEvent.onConnectionLostEvent(ignored.getLocalizedMessage());
                                break;
                            }
                        } catch (SocketException ignored){
                            System.err.println("Socket not connected");
                            System.err.println(ignored.getLocalizedMessage());
                            if(connectionLostEvent != null) connectionLostEvent.onConnectionLostEvent(ignored.getLocalizedMessage());
                            break;
                        } catch (EOFException ignored){
                            //System.err.println("EOFException");
                            //disconnect();
                            try {
                                in.reset();
                            } catch (IOException e) {

                            }
                        }catch (Exception e) {
                            System.err.println("Lukas du hurensohn was hast du getan dass dies ausgegeben wird");
                            System.err.println("Localized message: " + e.getLocalizedMessage());
                            e.printStackTrace();
                            if(connectionLostEvent != null) connectionLostEvent.onConnectionLostEvent(e.getLocalizedMessage());
                            try {
                                out.flush();
                            } catch (IOException ex) {}
                            break;
                        }
                    }
                    disconnect();
                }
            });
            t.start();
        } catch (SocketException e) {
            if(connectionFailEvent != null) connectionFailEvent.onConnectionFailedEvent(e);
            System.err.println("Couldnt connect, socket exception!");
            System.out.println(e.getLocalizedMessage());
        } catch (UnknownHostException e) {
            if(connectionFailEvent != null) connectionFailEvent.onConnectionFailedEvent(e);
            System.err.println("Unknown Host");
            System.out.println(e.getLocalizedMessage());
        } catch (IOException e) {
            if(connectionFailEvent != null) connectionFailEvent.onConnectionFailedEvent(e);
            e.printStackTrace();
        }
    }
    public static String lastObjectSendName = "";
    public static long lastSent = 0;
    public static void sendObject(Object o){
        try {
            toSend.add(o);
        }catch (Exception e){
            System.out.println("Clearing Buffer");
            toSend.clear();
        }
    }
    public static void startSender() throws Exception {
        final Object REFERENCE = new Object();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Started sender");
                while (true){
                    synchronized (REFERENCE){
                        if(!isConnected) {
                            toSend.clear();
                            continue;
                        }
                        if(toSend.isEmpty()) continue;
                        Object o = toSend.get(0);
                        toSend.remove(0);
                        if(o == null) continue;
                        lastSent = System.currentTimeMillis();
                        try {
                            lastObjectSendName = o.getClass().toString();
                            out.writeObject(o);
                            out.flush();
                            packetsSent++;
                        } catch (SocketException ignored){

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        t.start();
    }
    public static void setPacketListener(PacketListener packetListener){
        listener = packetListener;
    }
    public static void setConnectionFailedEvent(ConnectionFailedEvent listener){
        connectionFailEvent = listener;
    }
    public static void setOnConnectionSuccessEvent(ConnectionSuccessEvent listener){
        connectionSuccessEvent = listener;
    }
    public static void disconnect(){
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static int packetsInBuffer(){
        return toSend.size();
    }
}
