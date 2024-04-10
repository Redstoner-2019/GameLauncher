package me.redstoner2019.gamelauncher.client;

import me.redstoner2019.gamelauncher.packets.GamesPacket;
import me.redstoner2019.gamelauncher.packets.RequestGameVersions;
import me.redstoner2019.gamelauncher.packets.RequestGamesPacket;
import me.redstoner2019.gamelauncher.packets.VersionsPacket;
import me.redstoner2019.gamelauncher.packets.download.*;
import me.redstoner2019.serverhandling.*;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

    public static void main(String[] args) throws Exception {
        if(!new File("Client").exists()){
            new File("Client").mkdirs();
        }
        Scanner scanner = new Scanner(System.in);
        connect(scanner.nextLine(),1234);

        setPacketListener(new PacketListener() {
            @Override
            public void packetRecievedEvent(Object packet) {
                if(packet instanceof GamesPacket p){
                    System.out.println("Games Available");
                    for(String s : p.getGames()){
                        System.out.println(s);
                    }
                }
                if(packet instanceof VersionsPacket p){
                    System.out.println("Versions Available");
                    for(String s : p.getVersions()){
                        System.out.println(s);
                    }
                }
                if(packet instanceof DownloadUnavailablePacket p){
                    System.out.println("Download unavailable " + p.getReason());
                }
                if(packet instanceof DownloadHeader p){
                    data = new ArrayList<>();
                    System.out.println("Initializing Download of");
                    filename = p.getFilename();
                    System.out.println("Filename: " + p.getFilename());
                    bytes = 0;
                    System.out.println("Bytes: " + p.getBytes());
                    BYTES_PER_PACKET = p.getBytesPerPacket();
                    System.out.println("Bytes per packet: " + p.getBytesPerPacket());
                    packetsAwaiting = p.getPackets();
                    System.out.println("Packets: " + p.getPackets());
                    System.out.println("Recieving Packets");
                }
                if(packet instanceof DataPacket p){
                    data.add(p);
                }
                if(packet instanceof DownloadEndPacket){
                    if(!new File("ClientGames").exists()){
                        new File("ClientGames").mkdirs();
                    }
                    if(!new File("ClientGames/" + gameDownloading).exists()){
                        new File("ClientGames/" + gameDownloading).mkdirs();
                    }
                    System.out.println("Writing packets");

                    try {
                        FileOutputStream outputStream = new FileOutputStream("ClientGames/" + gameDownloading + "/" + filename);
                        for(DataPacket p : data){
                            outputStream.write(p.getData());
                        }
                        outputStream.close();

                        JSONObject object = new JSONObject();
                        object.put("version",versionDownloaded);
                        Util.writeStringToFile(Util.prettyJSON(object.toString()),new File("ClientGames/" + gameDownloading + "/downloadinfo.json"));
                    } catch (Exception e) {
                        System.out.println("An error occured");
                        e.printStackTrace();
                        return;
                    }
                    System.out.println("Download complete");
                }
            }
        });

        setConnectionFailedEvent(new ConnectionFailedEvent() {
            @Override
            public void onConnectionFailedEvent(Exception reason) {

            }
        });
        setConnectionLostEvent(new ConnectionLostEvent() {
            @Override
            public void onConnectionLostEvent(String reason) {

            }
        });
        setOnConnectionSuccessEvent(new ConnectionSuccessEvent() {
            @Override
            public void onConnectionSuccess() {
                sendObject(new RequestGamesPacket());
            }
        });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected()){
                    String command = scanner.nextLine();
                    if(command.startsWith("getversions")){
                        String game = command.substring("getversions ".length());
                        System.out.println("Requesting game versions " + game);
                        sendObject(new RequestGameVersions(game));
                    }else if(command.startsWith("getgames")){
                        System.out.println("Requesting games");
                        sendObject(new RequestGamesPacket());
                    }else if(command.startsWith("startdownload")){
                        System.out.println("Game?");
                        gameDownloading = scanner.nextLine();
                        System.out.println("Version?");
                        versionDownloaded = scanner.nextLine();
                        System.out.println("Requesting download of " + gameDownloading + " version " + versionDownloaded);
                        sendObject(new RequestDownloadPacket(gameDownloading,versionDownloaded));
                    }else if(command.startsWith("startupload")){
                        System.out.println("Game?");
                        String game = scanner.nextLine();
                        System.out.println("Version?");
                        String version = scanner.nextLine();
                        System.out.println("File location?");
                        String fileLocation = scanner.nextLine();
                        File file = new File(fileLocation);
                        if(!file.exists()){
                            System.out.println("File Not Found");
                            continue;
                        }
                        try {
                            long fileSize = Files.size(file.toPath());
                            int BYTES_PER_PACKET = 1024;
                            long packets = (fileSize/BYTES_PER_PACKET)+1;
                            sendObject(new DownloadHeader(packets,fileSize,BYTES_PER_PACKET,file.getName(),game,version));

                            int index = 0;

                            FileInputStream inputStream = new FileInputStream(file);
                            byte[] data = inputStream.readNBytes(BYTES_PER_PACKET);

                            while (data.length != 0){
                                sendObject(new DataPacket(index,data));
                                data = inputStream.readNBytes(BYTES_PER_PACKET);
                                index++;
                            }
                            sendObject(new DownloadEndPacket());
                            System.out.println(index + " packets sent.");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        System.out.println("Unknown command " + command);
                    }
                }
            }
        });
        t.start();

        startSender();
    }
}
