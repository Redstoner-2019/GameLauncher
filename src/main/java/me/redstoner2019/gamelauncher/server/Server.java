package me.redstoner2019.gamelauncher.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import me.redstoner2019.Main;
import me.redstoner2019.gamelauncher.packets.GamesPacket;
import me.redstoner2019.gamelauncher.packets.RequestGamesPacket;
import me.redstoner2019.gamelauncher.packets.download.*;
import me.redstoner2019.serverhandling.ClientConnectEvent;
import me.redstoner2019.serverhandling.ClientHandler;
import me.redstoner2019.serverhandling.PacketListener;
import me.redstoner2019.serverhandling.Util;
import org.json.JSONObject;
import org.json.JSONArray;

public class Server extends me.redstoner2019.serverhandling.Server {
    public static int PACKET_SIZE = 1024;
    public static List<DataPacket> recieve_data = new ArrayList<>();
    public static String recieve_filename = "";
    public static long recieve_bytes = 0;
    public static long recieve_BYTES_PER_PACKET = 0;
    public static long recieve_packetsAwaiting = 0;
    public static String recieve_gameDownloading = "";
    public static String recieve_versionDownloaded = "";
    public static JSONObject gameLocations;
    public static JSONObject gameList;
    public static File gameLocationsFile = new File("data/config/locations.json");
    public static File gameListFile = new File("data/config/games.json");

    public static void main(String[] args) throws Exception {
        File serverProperties = new File("server.properties");
        if(!serverProperties.exists()){
            serverProperties.createNewFile();
            JSONObject data = new JSONObject();
            data.put("port",8007);
            Util.writeStringToFile(data.toString(),serverProperties);
        }

        JSONObject data1 = new JSONObject(Util.readFile(serverProperties));
        int PORT = data1.getInt("port");
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
        if(!gameLocationsFile.exists()){
            gameLocationsFile.getParentFile().mkdirs();
            gameLocationsFile.createNewFile();
            Util.writeStringToFile("{ }",gameLocationsFile);
        }
        if(!gameListFile.exists()){
            gameListFile.getParentFile().mkdirs();
            gameListFile.createNewFile();
            Util.writeStringToFile("{ }",gameListFile);
        }

        /*try{
            System.out.println(new File("data/games").getCanonicalPath());
            new File("data/games").delete();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        gameList = new JSONObject();
        gameLocations = new JSONObject();
        save();
        System.out.println("Reset complete");*/

        gameLocations = new JSONObject(Util.readFile(gameLocationsFile));
        gameList = new JSONObject(Util.readFile(gameListFile));

        setClientConnectEvent(new ClientConnectEvent() {
            @Override
            public void connectEvent(ClientHandler handler) throws Exception {
                handler.startPacketSender();
                final ACK ack = new ACK(0);
                handler.startPacketListener(new PacketListener() {
                    @Override
                    public void packetRecievedEvent(Object packet) {
                        if(packet instanceof ACK){
                            ack.setSum(1);
                            synchronized (ack){
                                ack.notify();
                            }
                        }

                        if(packet instanceof DownloadHeader p){
                            recieve_data = new ArrayList<>();
                            recieve_filename = p.getFilename();
                            recieve_bytes = p.getBytes();
                            recieve_packetsAwaiting = p.getPackets();
                            recieve_gameDownloading = p.getGame();
                            recieve_versionDownloaded = p.getVersion();
                            recieve_BYTES_PER_PACKET = p.getBytesPerPacket();
                            if(hasVersion(recieve_gameDownloading,recieve_versionDownloaded)){
                                handler.sendObject(new DownloadUnavailablePacket("Version already exists"));
                            } else handler.sendObject(new ReadyForDownload());
                        }

                        if(packet instanceof DataPacket p){
                            long checksum = 0;
                            for(byte b : p.getData()){
                                checksum+=b;
                            }
                            handler.sendObject(new ACK(checksum));
                            recieve_data.add(p);
                        }

                        if(packet instanceof DownloadEndPacket p){
                            System.out.println("Writing packets");

                            addVersion(recieve_gameDownloading,recieve_versionDownloaded,recieve_filename,"Update " + recieve_versionDownloaded,"None",recieve_bytes);
                            if(gameExists(recieve_gameDownloading)){
                                try{
                                    String gameFile = getGameFile(recieve_gameDownloading,recieve_versionDownloaded);
                                    File file = new File(gameFile);
                                    if(!file.exists()){
                                        file.getParentFile().mkdirs();
                                        file.createNewFile();
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }

                            String gameFile = getGameFile(recieve_gameDownloading,recieve_versionDownloaded);
                            File file = new File(gameFile);
                            try {
                                FileOutputStream outputStream = new FileOutputStream(file);
                                long bytesWritten = 0;
                                long previousID = -1;
                                recieve_data.sort(new Comparator<DataPacket>() {
                                    @Override
                                    public int compare(DataPacket o1, DataPacket o2) {
                                        return o1.getId()-o2.getId();
                                    }
                                });
                                for(DataPacket pa : recieve_data){
                                    long checksum = 0;
                                    for(byte b : pa.getData()){
                                        checksum+=b;
                                    }
                                    if(checksum!=pa.getSum()) {
                                        System.out.println("Checksum error!");
                                        System.out.println("Packet Checksum: " + pa.getSum());
                                        System.out.println("Data Checksum: " + pa.getSum());
                                        return;
                                    }
                                    if(pa.getData().length != recieve_BYTES_PER_PACKET) {
                                        System.out.println("Invalid size");
                                        System.out.println("Size: " + pa.getData().length);
                                    }
                                    if(pa.getId()-1 != previousID){
                                        System.out.println("Order error");
                                        System.out.println("Prev: " + previousID);
                                        System.out.println("ID: " + pa.getId());
                                    }
                                    previousID = pa.getId();
                                    outputStream.write(pa.getData());
                                    bytesWritten+=pa.getData().length;
                                }
                                recieve_data.clear();
                                outputStream.close();
                                System.out.println("Complete");
                            } catch (Exception e) {
                                System.out.println("An error occured");
                                e.printStackTrace();
                                return;
                            }
                        }

                        if(packet instanceof RequestGamesPacket){
                            System.out.println("Request Games");
                            handler.sendObject(new GamesPacket(gameList.toString()));
                        }

                        if(packet instanceof RequestDownloadPacket p){
                            Thread downloadThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("Request Download " + p.getGame() + " " + gameExists(p.getGame()));
                                    if(gameExists(p.getGame())){
                                        String gameFile = getGameFile(p.getGame(),p.getVersion());
                                        try {
                                            File file = new File(gameFile);
                                            if(!file.exists()){
                                                file.getParentFile().mkdirs();
                                                file.createNewFile();
                                                FileOutputStream out = new FileOutputStream(file);
                                                Random random = new Random();
                                                byte[] bytes = new byte[1024];
                                                random.nextBytes(bytes);
                                                out.write(bytes);
                                                out.close();
                                            }

                                            long size = Files.size(file.toPath());

                                            PACKET_SIZE = (int) Math.max((double)size/500.0,Main.DEFAULT_SIZE);

                                            long packets = (long) Math.ceil((double) size /PACKET_SIZE);
                                            handler.sendObject(new DownloadHeader(packets,size,PACKET_SIZE,gameFile,p.getGame(),p.getVersion()));

                                            int index = 0;

                                            FileInputStream inputStream = new FileInputStream(file);
                                            byte[] data = inputStream.readNBytes(PACKET_SIZE);

                                            while (data.length != 0){
                                                handler.sendObject(new DataPacket(index,data));
                                                synchronized (ack){
                                                    try {
                                                        ack.wait(2000);
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                                if(ack.getSum() == 0){
                                                    //System.out.print("Packet loss - ");
                                                    continue;
                                                }
                                                ack.setSum(0);
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
                            });
                            downloadThread.start();
                        }
                    }
                });
            }
        });

        Thread consoleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                while (true){
                    String input = scanner.nextLine();
                    String command = input.split(" ")[0];
                    String[] arguments = new String[0];
                    if(input.length() > command.length()){
                        arguments = input.substring(command.length()+1).split(" ");
                    }
                    if(arguments.length == 0){
                        if(command.equalsIgnoreCase("hardreset")){
                            try{
                                System.out.println(new File("data/games").getCanonicalPath());
                                new File("data/games").delete();
                            } catch (Exception ex){
                                ex.printStackTrace();
                            }
                            gameList = new JSONObject();
                            gameLocations = new JSONObject();
                            save();
                            System.out.println("Reset complete");
                            continue;
                        }
                    } else if(arguments.length == 1) {
                        if(command.equalsIgnoreCase("size")){
                            Main.DEFAULT_SIZE = Integer.parseInt(arguments[0]);
                            System.out.println("Default Packet Size now " + Main.DEFAULT_SIZE);
                            continue;
                        }
                        if(command.equalsIgnoreCase("addgame")){
                            if(addGame(arguments[0])){
                                System.out.println("Successfully created game " + arguments[0]);
                            } else {
                                System.out.println("Failed to create game " + arguments[0]);
                            }
                            continue;
                        }
                    } else if(arguments.length == 2) {
                        if(command.equalsIgnoreCase("addversion")){
                            if(addVersion(arguments[0],arguments[1],arguments[0] + ".txt","Update " + arguments[1], "change1\nchange2\nchange3",recieve_bytes)){
                                System.out.println("Successfully added version " + arguments[1] + " to game " + arguments[0]);
                            } else {
                                System.out.println("Failed to add version " + arguments[1] + " to game " + arguments[0]);
                            }
                            continue;
                        }
                        if(command.equalsIgnoreCase("getfile")){
                            System.out.println(getGameFile(arguments[0],arguments[1]));
                            continue;
                        }
                    } else if(arguments.length == 3) {

                    } else if(arguments.length == 4) {

                    } else if(arguments.length == 5) {
                        if(command.equalsIgnoreCase("addversion")){
                            if(addVersion(arguments[0],arguments[1],arguments[2],arguments[3], arguments[4],0)){
                                System.out.println("Successfully added version " + arguments[1] + " to game " + arguments[0]);
                            } else {
                                System.out.println("Failed to add version " + arguments[1] + " to game " + arguments[0]);
                            }
                            continue;
                        }
                    }
                    System.err.println("Command not recognized");
                    System.err.println();
                    System.err.println("'hardreset' -> Resets everything");
                    System.err.println("'addgame [game]' -> adds a game");
                    System.err.println("'addversion [game] [version]' -> Adds a version to a game, creates game if not existing");
                }
            }
        });
        consoleThread.start();

        setup(PORT);
        start();
    }
    public static boolean addGame(String game){
        game = game.toLowerCase();
        if(gameList.has(game)){
            return false;
        } else {
            JSONObject gameObject = new JSONObject();
            gameObject.put("versions",new JSONArray());
            gameList.put(game,gameObject);

            JSONObject gameObjectFile = new JSONObject();
            gameObjectFile.put("versions",new JSONArray());
            gameLocations.put(game,gameObjectFile);

            save();
            return true;
        }
    }
    public static boolean addVersion(String game, String version, String filename, String updateTitle, String changes, long fileSize){
        game = game.toLowerCase();
        if(!gameExists(game)){
            if(!addGame(game)) return false;
        }
        if(!hasVersion(game,version)){
            JSONArray versions = gameList.getJSONObject(game.toLowerCase()).getJSONArray("versions");
            versions.put(version);

            JSONArray versionsLocations = gameLocations.getJSONObject(game.toLowerCase()).getJSONArray("versions");
            versionsLocations.put(version);

            JSONObject versionObject = new JSONObject();
            versionObject.put("file","data/games/" + game + "/" + version + "/" + filename);
            versionObject.put("title",updateTitle);
            versionObject.put("changes",changes);
            versionObject.put("size",fileSize);
            gameLocations.getJSONObject(game.toLowerCase()).put(version,versionObject);

            save();
            return true;
        } else {
            return false;
        }
    }
    public static boolean gameExists(String game){
        game = game.toLowerCase();
        return gameList.has(game.toLowerCase());
    }
    public static boolean hasVersion(String game, String version){
        game = game.toLowerCase();
        if(!gameExists(game)) return false;
        JSONArray versions = gameList.getJSONObject(game.toLowerCase()).getJSONArray("versions");
        for(Object o : versions){
            String v = (String) o;
            if(v.equals(version)) return true;
        }
        return false;
    }
    public static String getGameFile(String game, String version){
        game = game.toLowerCase();
        if(!hasVersion(game,version)) return null;
        JSONObject gameObject = gameLocations.getJSONObject(game);
        JSONObject versionsObject = gameObject.getJSONObject(version);
        return versionsObject.getString("file");
    }
    public static String getChanges(String game, String version){
        game = game.toLowerCase();
        if(!hasVersion(game,version)) return null;
        JSONObject gameObject = gameLocations.getJSONObject(game);
        JSONObject versionsObject = gameObject.getJSONObject(version);
        return versionsObject.getString("changes");
    }
    public static String getUpdateTitle(String game, String version){
        game = game.toLowerCase();
        if(!hasVersion(game,version)) return null;
        JSONObject gameObject = gameLocations.getJSONObject(game);
        JSONObject versionsObject = gameObject.getJSONObject(version);
        return versionsObject.getString("title");
    }
    public static void save(){
        try {
            Util.writeStringToFile(Util.prettyJSON(gameList.toString()),gameListFile);
            Util.writeStringToFile(Util.prettyJSON(gameLocations.toString()),gameLocationsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
