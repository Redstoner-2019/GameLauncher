package me.redstoner2019.serverhandling;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    private boolean connected = true;
    private List<Object> toSend = new ArrayList<>();

    public ClientHandler(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
        this.in = in;
        this.out = out;
        this.socket = socket;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public void setIn(ObjectInputStream in) {
        this.in = in;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public void setOut(ObjectOutputStream out) {
        this.out = out;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
    private int unuscessful_resets = 0;
    public void disconnect(){
        try {
            socket.close();
            Client.isConnected = false;
            connected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void startPacketSender(){
        final Object REFERENCE = new Object();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected()){
                    if(!isConnected() || socket.isClosed()) {
                        toSend.clear();
                        break;
                    }
                    if(toSend.isEmpty()) {
                        continue;
                    }
                    while (!toSend.isEmpty()){
                        Object o = toSend.get(0);
                        toSend.remove(0);
                        if(o == null) continue;
                        try {
                            synchronized (REFERENCE){
                                out.writeObject(o);
                                out.flush();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("Sender closed");
                Thread.currentThread().interrupt();
            }
        });
        t.start();
    }
    public void startPacketListener(final PacketListener listener){
        System.out.println("Started Packet Listener");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected()){
                    Object o = null;
                    if(socket.isClosed()){
                        Server.getClients().remove(this);
                        Util.log("Client disconnected"); //8008135
                        connected = false;
                        break;
                    }
                    try {
                        o = getIn().readObject();
                    } catch (ClassNotFoundException ignored) {
                        System.out.println("ClassNotFoundExeption");
                    } catch (EOFException ignored) {
                        System.out.println("EOFException");
                        try {
                            getIn().close();
                            setIn(new ObjectInputStream(socket.getInputStream()));
                            System.out.println(getIn());
                            unuscessful_resets = 0;
                        } catch (IOException e) {
                            System.out.println("Reset unsuccesful");
                            unuscessful_resets++;
                        }
                        if(unuscessful_resets == 10) {
                            disconnect();
                        }
                        break;
                    } catch (SocketException e) {
                        System.out.println(e.getLocalizedMessage());
                        if(e.getLocalizedMessage().equals("Connection reset")){
                            Server.getClients().remove(this);
                            Util.log("Client disconnected"); //8008135
                            connected = false;
                            break;
                        }
                    } catch (IOException e){
                        e.printStackTrace();
                        /*Server.getClients().remove(this);
                        Util.log("Client disconnected"); //8008135
                        run = false;
                        break;*/
                    }
                    listener.packetRecievedEvent((Packet) o);
                }
                System.out.println("End");
            }
        });
        t.start();
    }

    public void sendObject(Object packet){
        try {
            toSend.add(packet);
        }catch (Exception e){
            System.out.println("Clearing Buffer");
            toSend.clear();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public int packetsInBuffer(){
        return toSend.size();
    }
}
