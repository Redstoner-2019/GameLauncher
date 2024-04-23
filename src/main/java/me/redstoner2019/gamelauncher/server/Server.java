package me.redstoner2019.gamelauncher.server;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import me.redstoner2019.Main;
import me.redstoner2019.configuration.Configuration;
import me.redstoner2019.configuration.Game;
import me.redstoner2019.configuration.Type;
import me.redstoner2019.configuration.Version;
import me.redstoner2019.gamelauncher.packets.GamesPacket;
import me.redstoner2019.gamelauncher.packets.JSONRequest;
import me.redstoner2019.gamelauncher.packets.RequestGamesPacket;
import me.redstoner2019.gamelauncher.packets.download.*;
import me.redstoner2019.gamelauncher.packets.server.ConsoleLine;
import me.redstoner2019.gamelauncher.packets.server.StartConsolePacket;
import me.redstoner2019.gamelauncher.packets.server.StartServerPacket;
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
    public static String recieve_FILE_TYPE = "";
    public static Configuration configuration;
    public static File gameLocationsFile = new File("data/config/locations.json");
    public static HashMap<String, List<ClientHandler>> gameConsoleID = new HashMap<>();

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

        configuration = new Configuration(gameLocationsFile.getPath());
        configuration.load();

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

                        if(packet instanceof StartServerPacket p){
                            String id = p.getGame() + " - " + p.getVersion();
                            if(gameConsoleID.get(id) == null){
                                startServer(p.getGame(),p.getVersion(),handler);
                                for(String s : gameConsoleID.keySet()){
                                    if(gameConsoleID.get(s).contains(handler)){
                                        gameConsoleID.get(s).remove(handler);
                                        break;
                                    }
                                }
                                gameConsoleID.get(id).add(handler);
                            } else {
                                handler.sendObject(new StartConsolePacket());
                                for(String s : gameConsoleID.keySet()){
                                    if(gameConsoleID.get(s).contains(handler)){
                                        gameConsoleID.get(s).remove(handler);
                                        break;
                                    }
                                }
                                gameConsoleID.get(id).add(handler);
                            }
                        }

                        if(packet instanceof DownloadHeader p){
                            recieve_data = new ArrayList<>();
                            recieve_filename = p.getFilename();
                            recieve_bytes = p.getBytes();
                            recieve_packetsAwaiting = p.getPackets();
                            recieve_gameDownloading = p.getGame().toLowerCase();
                            recieve_versionDownloaded = p.getVersion();
                            recieve_BYTES_PER_PACKET = p.getBytesPerPacket();
                            recieve_FILE_TYPE = p.getFileType().toLowerCase();
                            System.out.println("Requested " + recieve_gameDownloading + " " + recieve_versionDownloaded + " " + recieve_FILE_TYPE);
                            if(!configuration.hasGame(recieve_gameDownloading)){
                                configuration.addGame(new Game(recieve_gameDownloading));
                            }
                            if(configuration.getGame(recieve_gameDownloading).hasVersion(recieve_versionDownloaded) && configuration.getGame(recieve_gameDownloading).getVersion(recieve_versionDownloaded).hasType(recieve_FILE_TYPE)){
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
                            Game game;
                            Version version;
                            Type type;

                            if(configuration.hasGame(recieve_gameDownloading)) {
                                game = configuration.getGame(recieve_gameDownloading);
                            } else {
                                game = new Game(recieve_gameDownloading);
                                configuration.addGame(game);
                            }

                            if(game.hasVersion(recieve_versionDownloaded)){
                                version = game.getVersion(recieve_versionDownloaded);
                                System.out.println("Version " + version);
                            } else {
                                version = new Version(recieve_versionDownloaded);
                                game.addVersion(version);
                            }

                            if(version.hasType(recieve_FILE_TYPE)){
                                type = version.getType(recieve_FILE_TYPE);
                                System.out.println("Filetype found");
                            } else {
                                type = new Type(recieve_FILE_TYPE); 
                                type.changes = "none";
                                type.title = "title";
                                type.size = recieve_bytes;
                                type.file = "data/games/" + game.name + "/" + version.name + "/" + type.name + "/" + recieve_filename;
                                version.addType(type);
                            }

                            if(!new File(type.file).exists()){
                                File file = new File(type.file);
                                file.getParentFile().mkdirs();
                                try {
                                    file.createNewFile();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            String gameFile = type.file;
                            File file = new File(gameFile);
                            boolean update = recieve_gameDownloading.equals("odlauncher") && type.name.equals("server");
                            File serverFile = new File(Main.jarPath);
                            try {
                                System.out.println("Creating streams");
                                System.out.println(serverFile);
                                FileOutputStream outputStream = new FileOutputStream(file);
                                FileOutputStream serverOutputStream = null;
                                if(update) serverOutputStream = new FileOutputStream(serverFile);
                                long bytesWritten = 0;
                                long previousID = -1;

                                int ind = 0;
                                for(DataPacket pa : recieve_data){
                                    ind++;
                                    outputStream.write(pa.getData());
                                    if(update) serverOutputStream.write(pa.getData());
                                    bytesWritten+=pa.getData().length;
                                }
                                recieve_data.clear();
                                outputStream.close();
                                if(update) System.out.println("Updated");
                                if(update) serverOutputStream.close();
                                System.out.println("Complete");
                                configuration.save();
                                if(update) {
                                    System.out.println("Restarting");
                                    Main.restart("screen ");
                                }
                            } catch (Exception e) {
                                System.out.println("An error occured");
                                e.printStackTrace();
                                return;
                            }
                        }

                        if(packet instanceof JSONRequest p){
                            JSONObject object = new JSONObject(p.getJson());
                            switch (object.getString("header")){
                                case "request": {
                                    switch (object.getString("data")){
                                        case "client-update" : {
                                            JSONObject data = new JSONObject();
                                            data.put("header","update-response");
                                            if(configuration.hasGame("odlauncher")){
                                                if(!configuration.getGame("odlauncher").versions.isEmpty())data.put("data",configuration.getGame("odlauncher").versions.get(configuration.getGame("odlauncher").versions.size()-1).name);
                                                handler.sendObject(new JSONRequest(data.toString()));
                                            } else {
                                                handler.sendObject(new DownloadUnavailablePacket("Updates not yet setup on the Server"));
                                            }
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }

                        if(packet instanceof RequestGamesPacket){
                            System.out.println("Request Games");
                            JSONObject games = new JSONObject();

                            for(Game game : configuration.games){
                                //if(game.name.equals("odlauncher")) continue;
                                JSONArray versions = new JSONArray();
                                JSONObject gameO = new JSONObject();
                                for(Version v : game.versions){
                                    JSONArray types = new JSONArray();
                                    for(Type t : v.types){
                                        types.put(t.name);
                                    }
                                    gameO.put(v.name,types);
                                    versions.put(v.name);
                                }
                                gameO.put("versions",versions);
                                gameO.put("full-data",new JSONObject(configuration.toString()).getJSONObject(game.name));
                                games.put(game.name,gameO);
                                /*JSONObject sizeVersionJSON = new JSONObject();
                                for(Version v : game.versions){
                                    JSONObject sizeJSON = new JSONObject();
                                    for(Type t : v.types){
                                        sizeJSON.put(t.name,t.size);
                                    }
                                    sizeVersionJSON.put(v.name,sizeJSON);
                                }
                                gameO.put("sizes",sizeVersionJSON);*/

                            }
                            handler.sendObject(new GamesPacket(games.toString()));
                        }

                        if(packet instanceof RequestDownloadPacket p){
                            Thread downloadThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        Game game = configuration.getGame(p.getGame());
                                        Version version = game.getVersion(p.getVersion());
                                        Type type = version.getType(p.getType());
                                        System.out.println("Request Download " + p.getGame() + " " + configuration.hasGame(p.getGame()));
                                        if(configuration.hasGame(p.getGame())){
                                            String gameFile = type.file;
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
                                                handler.sendObject(new DownloadHeader(packets,size,PACKET_SIZE,game.name + "/" + type.name + "/" + new File(type.file).getName(),p.getGame(),p.getVersion(),p.getType()));

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
                                    }catch (Exception e){
                                        handler.sendObject(new DownloadUnavailablePacket("Server Error"));
                                    }

                                }
                            });
                            downloadThread.start();
                        }
                    }
                });
            }
        });

        /*Thread consoleThread = new Thread(new Runnable() {
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
                            if(addVersion(arguments[0],arguments[1],arguments[0] + ".txt","Update " + arguments[1], "change1\nchange2\nchange3",recieve_bytes,"none")){
                                System.out.println("Successfully added version " + arguments[1] + " to game " + arguments[0]);
                            } else {
                                System.out.println("Failed to add version " + arguments[1] + " to game " + arguments[0]);
                            }
                            continue;
                        }
                        if(command.equalsIgnoreCase("getfile")){
                            //System.out.println(getGameFile(arguments[0],arguments[1]));
                            continue;
                        }
                    } else if(arguments.length == 3) {

                    } else if(arguments.length == 4) {

                    } else if(arguments.length == 5) {
                        if(command.equalsIgnoreCase("addversion")){
                            if(addVersion(arguments[0],arguments[1],arguments[2],arguments[3], arguments[4],0,"none")){
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
        consoleThread.start();*/

        setup(PORT);
        start();
    }
    public static void startServer(String game, String version, ClientHandler handler){
        if(gameConsoleID.get(game + " - " + version) == null){
            System.out.println("game " + game);
            System.out.println("version " + version);
            if(!configuration.getGame(game).getVersion(version).hasType("server")){
                handler.sendObject(new DownloadUnavailablePacket("This program does not have a server!"));
                return;
            }
            File file = new File(configuration.getGame(game).getVersion(version).getType("server").file);
            try {
                Process process = Runtime.getRuntime().exec("java -jar " + file.getAbsolutePath(),null, file.getParentFile());
                gameConsoleID.put(game + " - " + version,new ArrayList<>());
                handler.sendObject(new StartConsolePacket());
                Thread consoleThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        Scanner inputStream = new Scanner(process.getInputStream());
                        while (process.isAlive()){
                            try{
                                String line = inputStream.nextLine();
                                for(ClientHandler h : gameConsoleID.get(game + " - " + version)){
                                    h.sendObject(new ConsoleLine(line));
                                }
                            } catch (Exception e){
                                for(ClientHandler h : gameConsoleID.get(game + " - " + version)){
                                    h.sendObject(new ConsoleLine("Process finished with exit code " + process.exitValue()));
                                }
                                break;
                            }
                        }
                    }
                });
                consoleThread.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            handler.sendObject(new DownloadUnavailablePacket("Server already running"));
        }
        System.out.println("Start complete");
    }
}
