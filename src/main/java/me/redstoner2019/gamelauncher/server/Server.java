package me.redstoner2019.gamelauncher.server;

import java.io.File;
import java.io.FileInputStream;
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
                                    handler.sendObject(new DownloadHeader(packets,Files.size(Paths.get(file)),PACKET_SIZE,gameFile));

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
