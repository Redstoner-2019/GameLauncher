package me.redstoner2019.gamelauncher.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import me.redstoner2019.gamelauncher.packets.GamesPacket;
import me.redstoner2019.gamelauncher.packets.RequestGameVersions;
import me.redstoner2019.gamelauncher.packets.RequestGamesPacket;
import me.redstoner2019.gamelauncher.packets.VersionsPacket;
import me.redstoner2019.gamelauncher.packets.download.*;
import me.redstoner2019.serverhandling.ClientConnectEvent;
import me.redstoner2019.serverhandling.ClientHandler;
import me.redstoner2019.serverhandling.PacketListener;
import me.redstoner2019.serverhandling.Util;
import org.json.JSONObject;
import org.json.JSONArray;

public class Server extends me.redstoner2019.serverhandling.Server {
    public static File configFile = new File("data/config/config.json");
    public static final int PACKET_SIZE = 1024;


    public static List<DataPacket> recieve_data = new ArrayList<>();
    public static String recieve_filename = "";
    public static long recieve_bytes = 0;
    public static long recieve_BYTES_PER_PACKET = 0;
    public static long recieve_packetsAwaiting = 0;
    public static String recieve_gameDownloading = "";
    public static String recieve_versionDownloaded = "";

    public static void main(String[] args) throws Exception {
        if(!configFile.exists()){
            new File("data/config").mkdirs();
            configFile.createNewFile();
            JSONObject object = new JSONObject();

            JSONObject gameObject = new JSONObject();
            gameObject.put("name","Minecraft");
            gameObject.put("filename","minecraft.exe");
            JSONArray versions = new JSONArray();
            versions.put("1.0");
            versions.put("1.1");
            gameObject.put("versions",versions);

            object.put("Minecraft",gameObject);

            gameObject.put("name","GTA");
            gameObject.put("filename","gta.exe");
            gameObject.put("versions",versions);

            object.put("GTA",gameObject);

            Util.writeStringToFile(Util.prettyJSON(object.toString()),configFile);
        }

        JSONObject serverData = new JSONObject(Util.readFile(configFile));

        setClientConnectEvent(new ClientConnectEvent() {
            @Override
            public void connectEvent(ClientHandler handler) throws Exception {
                handler.startPacketSender();

                handler.startPacketListener(new PacketListener() {
                    @Override
                    public void packetRecievedEvent(Object packet) {
                        if(packet instanceof DownloadHeader p){
                            recieve_data = new ArrayList<>();
                            recieve_filename = p.getFilename();
                            recieve_bytes = p.getBytes();
                            recieve_packetsAwaiting = p.getPackets();
                            recieve_gameDownloading = p.getGame();
                            recieve_versionDownloaded = p.getVersion();
                            recieve_BYTES_PER_PACKET = p.getBytesPerPacket();
                        }
                        if(packet instanceof DataPacket p){
                            recieve_data.add(p);
                        }
                        if(packet instanceof DownloadEndPacket p){
                            if(!new File("data/Games/").exists()){
                                new File("data/Games/").mkdirs();
                            }
                            if(!new File("data/Games/" + recieve_gameDownloading).exists()){
                                new File("data/Games/" + recieve_gameDownloading).mkdirs();
                            }
                            if(!new File("data/Games/" + recieve_gameDownloading + "/" + recieve_versionDownloaded.replaceAll("\\.","_")).exists()){
                                new File("data/Games/" + recieve_gameDownloading + "/" + recieve_versionDownloaded.replaceAll("\\.","_")).mkdirs();
                            }
                            System.out.println("Writing packets");

                            try {
                                FileOutputStream outputStream = new FileOutputStream("data/Games/" + recieve_gameDownloading + "/" + recieve_versionDownloaded.replaceAll("\\.","_") + "/" + recieve_filename);
                                for(DataPacket pa : recieve_data){
                                    outputStream.write(pa.getData());
                                }
                                outputStream.close();

                                JSONArray versions = serverData.getJSONObject(recieve_gameDownloading).getJSONArray("versions");
                                versions.put(recieve_versionDownloaded);
                                Util.writeStringToFile(Util.prettyJSON(serverData.toString()),configFile);
                                System.out.println("Complete");
                            } catch (Exception e) {
                                System.out.println("An error occured");
                                e.printStackTrace();
                                return;
                            }
                        }
                        if(packet instanceof RequestGamesPacket){
                            System.out.println("Request Games");
                            List<String> games = new ArrayList<>();
                            games.addAll(serverData.keySet());
                            handler.sendObject(new GamesPacket(games));
                        }
                        if(packet instanceof RequestGameVersions p){
                            System.out.println("Request Versions " + p.getGame() + " " + serverData.has(p.getGame()));
                            if(serverData.has(p.getGame())){
                                List<String> versions = new ArrayList<>();
                                for(Object s : serverData.getJSONObject(p.getGame()).getJSONArray("versions")){
                                    versions.add((String) s);
                                }
                                handler.sendObject(new VersionsPacket(versions));
                            }
                        }
                        if(packet instanceof RequestDownloadPacket p){
                            System.out.println("Request Download " + p.getGame() + " " + serverData.has(p.getGame()));
                            if(serverData.has(p.getGame())){
                                JSONObject game = serverData.getJSONObject(p.getGame());
                                String gameFile = game.getString("filename");
                                try {
                                    String file = "data/Games/" + p.getGame() + "/" + (p.getVersion().replaceAll("\\.","_")) + "/" + gameFile;
                                    long packets = (Files.size(Paths.get(file))/PACKET_SIZE) + 1;
                                    handler.sendObject(new DownloadHeader(packets,Files.size(Paths.get(file)),PACKET_SIZE,gameFile,p.getGame(),p.getVersion()));

                                    int index = 0;

                                    FileInputStream inputStream = new FileInputStream(file);
                                    byte[] data = inputStream.readNBytes(PACKET_SIZE);

                                    while (data.length != 0){
                                        handler.sendObject(new DataPacket(index,data));
                                        data = inputStream.readNBytes(PACKET_SIZE);
                                        index++;
                                    }
                                    handler.sendObject(new DownloadEndPacket());
                                    System.out.println(index + " packets sent.");
                                } catch (IOException e) {
                                    System.out.println(e.getLocalizedMessage());
                                    handler.sendObject(new DownloadUnavailablePacket("Server Error"));
                                }
                            } else {
                                handler.sendObject(new DownloadUnavailablePacket("Game Not Found"));
                            }
                        }
                    }
                });
            }
        });

        setup(1234);
        start();
    }
}
