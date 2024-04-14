package me.redstoner2019.gamelauncher.client;

import me.redstoner2019.Main;
import me.redstoner2019.gamelauncher.packets.GamesPacket;
import me.redstoner2019.gamelauncher.packets.RequestGamesPacket;
import me.redstoner2019.gamelauncher.packets.VersionsPacket;
import me.redstoner2019.gamelauncher.packets.download.*;
import me.redstoner2019.serverhandling.*;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client extends me.redstoner2019.serverhandling.Client {
    public static List<DataPacket> data = new ArrayList<>();
    public static String filename = "";
    public static long bytes = 0;
    public static long BYTES_PER_PACKET = 0;
    public static long packetsAwaiting = 0;
    public static String gameDownloading = "";
    public static String versionDownloaded = "";
    private static JFrame frame;
    private static int width = 1280;
    private static int height = 720;
    public static JList<String> gamesJList = new JList<>();
    public static JList<String> versionsJList = new JList<>();
    public static JProgressBar progressBar = new JProgressBar();
    public static JSONObject games = new JSONObject();
    public static String[] gamesData = new String[0];
    public static String[] versionData = new String[0];
    public static final ACK ack = new ACK(0);
    public static long bytesRecieved = 0;
    public static JLabel connectionStatus = new JLabel("Not connected");
    public static JLabel titleLabel = new JLabel("OD Launcher");
    public static JTextField fileServer = new JTextField("localhost");
    public static JButton refreshButton = new JButton("Refresh");
    public static JButton downloadButton = new JButton("Download");
    public static JButton connectButton = new JButton("Connect");
    public static JButton disconnectButton = new JButton("Disconnect");
    public static JButton launchSelected = new JButton("Launch");
    public static JScrollPane gameScrollPane = new JScrollPane(gamesJList);
    public static JScrollPane versionScrollPane = new JScrollPane(versionsJList);
    public static JButton uploadFile = new JButton("Upload new Version");

    public static void main(String[] args) throws Exception {
        Thread gcThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.gc();
                }
            }
        });
        gcThread.start();
        if(!new File("Client").exists()){
            new File("Client").mkdirs();
        }

        File clientProperties = new File("client.properties");
        if(!clientProperties.exists()){
            clientProperties.createNewFile();
            JSONObject data = new JSONObject();
            data.put("ip","localhost");
            Util.writeStringToFile(data.toString(),clientProperties);
        }

        JSONObject data1 = new JSONObject(Util.readFile(clientProperties));
        if(data1.has("ip")) fileServer.setText(data1.getString("ip"));

        Thread gui = new Thread(new Runnable() {
            @Override
            public void run() {
                initialize();
            }
        });
        gui.start();

        /**
         * Sending variables
         */
        final long[] lastSpeedUpdate = {0};
        final double[] lastSpeed = {0};

        fileServer.setEnabled(true);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        refreshButton.setEnabled(false);
        downloadButton.setEnabled(false);
        uploadFile.setEnabled(false);
        launchSelected.setEnabled(false);

        setConnectionLostEvent(new ConnectionLostEvent() {
            @Override
            public void onConnectionLostEvent(String reason) {
                System.out.println("connection lost " + reason);
                connectionStatus.setForeground(Color.RED);
                connectionStatus.setText("connection lost: " + reason);

                fileServer.setEnabled(true);
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                refreshButton.setEnabled(false);
                downloadButton.setEnabled(false);
                uploadFile.setEnabled(false);
                launchSelected.setEnabled(false);
            }
        });
        setConnectionFailedEvent(new ConnectionFailedEvent() {
            @Override
            public void onConnectionFailedEvent(Exception reason) {
                System.out.println("connection failed " + reason);
                connectionStatus.setForeground(Color.RED);
                connectionStatus.setText("connection failed: " + reason);

                fileServer.setEnabled(true);
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                refreshButton.setEnabled(false);
                downloadButton.setEnabled(false);
                uploadFile.setEnabled(false);
                launchSelected.setEnabled(false);
            }
        });
        setOnConnectionSuccessEvent(new ConnectionSuccessEvent() {
            @Override
            public void onConnectionSuccess() {
                System.out.println("Connected");
                connectionStatus.setForeground(Color.GREEN);
                connectionStatus.setText("connected");

                fileServer.setEnabled(false);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                refreshButton.setEnabled(true);
                downloadButton.setEnabled(true);
                uploadFile.setEnabled(true);
                launchSelected.setEnabled(true);
            }
        });

        setPacketListener(new PacketListener() {
            @Override
            public void packetRecievedEvent(Object packet) {
                if(packet instanceof ReadyForDownload){
                    ack.setSum(1);
                    synchronized (ack){
                        ack.notify();
                    }
                }
                if(packet instanceof ACK){
                    ack.setSum(1);
                    synchronized (ack){
                        ack.notify();
                    }
                }
                if(packet instanceof DownloadHeader p){
                    data = new ArrayList<>();
                    System.out.println("Initializing Download of");
                    versionDownloaded = p.getVersion();
                    gameDownloading = p.getGame();
                    filename = p.getFilename();
                    System.out.println("Filename: " + p.getFilename());
                    bytes = 0;
                    System.out.println("Bytes: " + p.getBytes());
                    BYTES_PER_PACKET = p.getBytesPerPacket();
                    System.out.println("Bytes per packet: " + p.getBytesPerPacket());
                    packetsAwaiting = p.getPackets();
                    System.out.println("Packets: " + p.getPackets());
                    progressBar.setMaximum((int) (p.getPackets()*2));
                    progressBar.setValue(0);
                    progressBar.setString("Recieving 0/" + p.getPackets() + " - 0.0%");
                    bytesRecieved = 0;
                    sendObject(new ReadyForDownload());
                    System.out.println("Recieving Packets");
                }
                if(packet instanceof DataPacket p){
                    long checksum = 0;
                    for(byte b : p.getData()){
                        checksum+=b;
                    }
                    sendObject(new ACK(checksum));
                    data.add(p);
                    bytesRecieved += p.getData().length;
                    progressBar.setValue(progressBar.getValue()+1);
                    if(System.currentTimeMillis() - lastSpeedUpdate[0] > 100){
                        lastSpeed[0] = bytesToMBit(bytesRecieved*10);
                        bytesRecieved = 0;
                        lastSpeedUpdate[0] = System.currentTimeMillis();
                    }
                    progressBar.setString(String.format("Recieving " + data.size() + "/" + packetsAwaiting + " - %.2f %% (Total %.2f %%) - %.2f MBit/s",((double) (p.getId() + 1) /(double) packetsAwaiting) * 100.0,((double)progressBar.getValue()/(double)progressBar.getMaximum()) * 100.0,lastSpeed[0]));
                }
                if(packet instanceof DownloadEndPacket){
                    System.out.println("Writing packets");
                    File out = new File("ClientGames/" + filename);
                    File outputFile = new File(out.getParent().replaceAll("\\.","_") + "/" + out.getName());
                    if(!outputFile.exists()){
                        outputFile.getParentFile().mkdirs();
                        try {
                            outputFile.createNewFile();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        for(DataPacket p : data){
                            outputStream.write(p.getData());
                            progressBar.setValue(progressBar.getValue()+1);
                            bytesRecieved += p.getData().length;
                            if(System.currentTimeMillis() - lastSpeedUpdate[0] > 100){
                                lastSpeed[0] = bytesToMBit(bytesRecieved*10);
                                bytesRecieved = 0;
                                lastSpeedUpdate[0] = System.currentTimeMillis();
                            }
                            progressBar.setString(String.format("Writing " + (p.getId() + 1) + "/" + data.size() + " - %.2f %% (Total %.2f %%) - %.2f MBit/s",((double) (p.getId() + 1) /(double) data.size()) * 100.0,((double)progressBar.getValue()/(double)progressBar.getMaximum()) * 100.0,lastSpeed[0]));
                        }
                        data.clear();
                        outputStream.close();

                        JSONObject object = new JSONObject();
                        object.put("version",versionDownloaded);
                        object.put("filename",outputFile.getName());
                        Util.writeStringToFile(Util.prettyJSON(object.toString()),new File(outputFile.getParentFile() + "/downloadinfo.json"));
                    } catch (Exception e) {
                        System.out.println("An error occured");
                        e.printStackTrace();
                        return;
                    }
                    System.out.println("Download complete");
                }
                if(packet instanceof GamesPacket p){
                    System.out.println("---------------");
                    System.out.println("Games Available");
                    System.out.println("---------------");
                    games = new JSONObject(p.getGamesJSON());
                    //System.out.println(Util.prettyJSON(p.getGamesJSON()));

                    setGamesData();

                    if(gamesData.length > 0) setVersionData(gamesJList.getSelectedValue());

                    System.out.println();
                }
                if(packet instanceof DownloadUnavailablePacket p){
                    System.out.println("Download unavailable " + p.getReason());
                    connectionStatus.setForeground(Color.RED);
                    connectionStatus.setText("failed: " + p.getReason());
                }
            }
        });
        startSender();
    }

    private static void initialize() {
        frame = new JFrame();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, width, height);
        frame.setTitle("OD Launcher");
        frame.setLocationRelativeTo(null);

        JPanel pan = new JPanel();
        frame.getContentPane().add(pan, BorderLayout.CENTER);
        pan.setLayout(null);

        JLabel panel = new JLabel();
        panel.setBounds(0,0,width-16,height-39);
        pan.add(panel);

        titleLabel.setBounds(0,0,panel.getWidth(),50);
        fileServer.setBounds(925,50,panel.getWidth()-950,30);
        connectButton.setBounds(925,100,panel.getWidth()-950,30);
        disconnectButton.setBounds(925,150,panel.getWidth()-950,30);
        refreshButton.setBounds(925,200,panel.getWidth()-950,30);
        downloadButton.setBounds(925,250,panel.getWidth()-950,30);
        uploadFile.setBounds(925,300,panel.getWidth()-950,30);
        gameScrollPane.setBounds(50,50,400,panel.getHeight()-100);
        versionScrollPane.setBounds(500,50,400,panel.getHeight()-100);
        progressBar.setBounds(50,panel.getHeight()-40,850,30);
        connectionStatus.setBounds(900,panel.getHeight()-40,panel.getWidth()-900,30);
        launchSelected.setBounds(925,panel.getHeight()-100,panel.getWidth()-950,30);

        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setVerticalAlignment(JLabel.CENTER);
        connectionStatus.setHorizontalAlignment(JLabel.CENTER);
        connectionStatus.setVerticalAlignment(JLabel.CENTER);

        connectionStatus.setForeground(Color.RED);

        titleLabel.setFont(new Font(titleLabel.getFont().getFontName(),Font.PLAIN,30));

        launchSelected.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String game = gamesJList.getSelectedValue();
                String version = versionsJList.getSelectedValue();

                if(version == null || game == null){
                    JOptionPane.showMessageDialog(frame,"Invailid game or version");
                    return;
                }

                File jar = new File("ClientGames/data/games/"+game+"/"+version.replaceAll("\\.","_")+"/downloadinfo.json");
                if(!jar.exists()){
                    startDownload(game,version);
                    if(!jar.exists()){
                        return;
                    }
                }
                try {
                    JSONObject gameData = new JSONObject(Util.readFile(jar));
                    jar = new File("ClientGames/data/games/"+game+"/"+version.replaceAll("\\.","_") + "/" + gameData.getString("filename"));
                    if(jar.getName().endsWith(".jar")){
                        String[] commands = {"java -jar " + jar.getAbsolutePath()};
                        System.out.println(Arrays.toString(commands));
                        File finalJar = jar;
                        Thread gameThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //Process process = Runtime.getRuntime().exec(commands[0]);

                                    new File(finalJar.getParent() + "/temp").mkdirs();

                                    System.setProperty("java.io.tmpdir", finalJar.getParent() + "/temp");

                                    frame.setVisible(false);

                                    Process process = Runtime.getRuntime().exec(commands[0],null, finalJar.getParentFile());

                                    process.waitFor();
                                    new File("ClientGames/data/games/"+game+"/"+version.replaceAll("\\.","_") + "/logs/").mkdirs();
                                    FileOutputStream outputStream = new FileOutputStream("ClientGames/data/games/"+game+"/"+version.replaceAll("\\.","_") + "/logs/dump.log");

                                    outputStream.write(process.getErrorStream().readAllBytes());
                                    outputStream.write('\n');
                                    outputStream.write('\n');
                                    outputStream.write('\n');
                                    outputStream.write('\n');
                                    outputStream.write(process.getInputStream().readAllBytes());

                                    frame.setVisible(true);

                                    outputStream.close();
                                } catch (IOException | InterruptedException ex) {
                                    frame.setVisible(true);
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
                        gameThread.start();
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        uploadFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setValue(0);
                        JFileChooser chooser = new JFileChooser("C:\\Users\\andre\\Downloads\\uploadtest");
                        chooser.showOpenDialog(frame);

                        File file = chooser.getSelectedFile();

                        String game = JOptionPane.showInputDialog("Game?");
                        String version = JOptionPane.showInputDialog("Version?");

                        if(game == null || version == null) {
                            System.out.println("Cancelled");
                            return;
                        }

                        if(!file.exists()){
                            System.out.println("File not found");
                            return;
                        }
                        try {
                            long fileSize = Files.size(file.toPath());

                            int BYTES_PER_PACKET_SENDING = (int) Math.max((double)fileSize/500.0, Main.DEFAULT_SIZE);
                            System.out.println(BYTES_PER_PACKET_SENDING + " packet size");

                            long packets = (long) Math.ceil((double) fileSize / BYTES_PER_PACKET_SENDING);

                            progressBar.setMaximum((int) packets);

                            sendObject(new DownloadHeader(packets,fileSize,BYTES_PER_PACKET_SENDING,file.getName(),game,version));

                            System.out.println("File size " + Files.size(file.toPath()) + " bytes");

                            int index = 0;

                            FileInputStream inputStream = new FileInputStream(file);
                            byte[] data = inputStream.readNBytes(BYTES_PER_PACKET_SENDING);
                            synchronized (ack){
                                try {
                                    ack.wait();
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                            ack.setSum(0);

                            final long[] lastSpeedUpdate = {0};
                            final double[] lastSpeed = {0};

                            while (data.length != 0){
                                sendObject(new DataPacket(index,data));
                                synchronized (ack){
                                    try {
                                        ack.wait(2000);
                                    } catch (InterruptedException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                                if(ack.getSum() == 0){
                                    System.out.println("Packet loss");
                                    continue;
                                }
                                bytesRecieved+=data.length;
                                progressBar.setValue(progressBar.getValue()+1);
                                if(System.currentTimeMillis() - lastSpeedUpdate[0] > 100){
                                    lastSpeed[0] = bytesToMBit(bytesRecieved*10);
                                    bytesRecieved = 0;
                                    lastSpeedUpdate[0] = System.currentTimeMillis();
                                }
                                index++;
                                progressBar.setString(String.format("Sending " + progressBar.getValue() + "/" + progressBar.getMaximum() + " - %.2f %% - %.2f MBit/s",((double) (progressBar.getValue()) /(double) progressBar.getMaximum()) * 100.0,lastSpeed[0]));
                                ack.setSum(0);
                                data = inputStream.readNBytes(BYTES_PER_PACKET_SENDING);
                            }
                            sendObject(new DownloadEndPacket());
                            System.out.println(index + " packets sent.");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        refreshFiles();
                        uploadFile.setEnabled(true);
                    }
                });
                t.start();
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(gamesJList.getSelectedValue() != null && versionsJList.getSelectedValue() != null) {
                    refreshFiles();
                    startDownload(gamesJList.getSelectedValue(),versionsJList.getSelectedValue());
                }
                else System.out.println("Nothing selected");
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshFiles();
            }
        });

        gamesJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                System.out.println(gamesJList.getSelectedIndex() + " - " + gamesJList.getSelectedValue());
                if(gamesJList.getSelectedValue() != null) setVersionData(gamesJList.getSelectedValue());
            }
        });
        versionsJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

            }
        });
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JSONObject data = new JSONObject();
                data.put("ip",fileServer.getText());
                String[] ip = fileServer.getText().split(":");
                try {
                    Util.writeStringToFile(data.toString(),new File("client.properties"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                if(ip.length == 2){
                    connect(ip[0],Integer.parseInt(ip[1]));
                } else {
                    connect(fileServer.getText(),8007);
                }
                refreshFiles();
            }
        });
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        panel.add(titleLabel);
        panel.add(fileServer);
        panel.add(refreshButton);
        panel.add(downloadButton);
        panel.add(gameScrollPane);
        panel.add(versionScrollPane);
        panel.add(progressBar);
        panel.add(uploadFile);
        panel.add(connectionStatus);
        panel.add(connectButton);
        panel.add(disconnectButton);
        panel.add(launchSelected);

        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        frame.setVisible(true);
    }
    public static void refreshFiles(){
        if(!isConnected()) {return;}

        sendObject(new RequestGamesPacket());
    }
    public static void startDownload(String game, String version){
        if(!isConnected()) {return;}

        sendObject(new RequestDownloadPacket(game,version));
    }
    public static void setGamesData(){
        int selectedIndex = gamesJList.getSelectedIndex();

        if(selectedIndex < 0) selectedIndex = 0;

        gamesData = new String[games.length()];
        int index = 0;
        for(String s : games.keySet()){
            gamesData[index] = s;
            index++;
        }
        gamesJList.setListData(gamesData);

        if(gamesData.length > 0) gamesJList.setSelectedIndex(selectedIndex);
    }
    public static void setVersionData(String game){
        if(gamesData.length > 0){
            int selectedIndex = versionsJList.getSelectedIndex();

            if(selectedIndex < 0) selectedIndex = 0;

            versionData = new String[games.getJSONObject(game).getJSONArray("versions").length()];

            int index = 0;
            for(Object o : games.getJSONObject(game).getJSONArray("versions")){
                String version = (String) o;
                versionData[index] = version;
                index++;
            }

            versionsJList.setListData(versionData);

            if(versionData.length > 0){
                versionsJList.setSelectedIndex(selectedIndex);
            }
        }
    }
    public static double bytesToKB(long bytes){
        return ((double) bytes /1024.0);
    }
    public static double bytesToMB(long bytes){
        return ((double) bytes /1024.0/1024.0);
    }
    public static double bytesToMBit(long bytes){
        return ((double) (bytes*8) /1024.0/1024.0);
    }
}
