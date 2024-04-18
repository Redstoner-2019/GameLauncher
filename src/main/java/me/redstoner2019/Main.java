package me.redstoner2019;

import me.redstoner2019.configuration.Configuration;
import me.redstoner2019.configuration.Game;
import me.redstoner2019.configuration.Type;
import me.redstoner2019.configuration.Version;
import me.redstoner2019.gamelauncher.client.Client;
import me.redstoner2019.gamelauncher.server.Server;
import me.redstoner2019.serverhandling.ClientHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Main {
    public static int DEFAULT_SIZE = 131072;
    public static String jarPath = "";

    static {
        try {
            jarPath = me.redstoner2019.serverhandling.Client.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        /*Configuration configuration = new Configuration("configuration.json");
        configuration.load();
        Version version = configuration.getGame("Uno").getVersion("1.0");
        version.addType(new Type("server"));
        configuration.save();*/


        if(args.length == 1){
            if(args[0].equalsIgnoreCase("server")) {
                Server.main(args);
                return;
            }
        }
        Client.main(args);
    }
    public static void restart(String prefix) throws IOException {
        if(true) return;
        System.out.println("Closing socket");
        //Server.serverSocket.close();
        System.out.println("Starting new Server");
        prefix = "";
        Process process = Runtime.getRuntime().exec(prefix + "screen java -jar " + jarPath.substring(1));
        System.out.println("New Server Started");

        try{
            int i = 0;
            while (true){
                Scanner scanner = new Scanner(process.getInputStream());
                System.out.println("NEW: " + scanner.nextLine());
                i++;
                if(i == 3) break;
            }
            for(ClientHandler h : Server.getClients()) h.disconnect();
            while (true){}
            //System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
            for(ClientHandler h : Server.getClients()) h.disconnect();
            //System.exit(0);
        }
        //System.exit(0);
    }

    public static void restart() throws IOException {
        Process process = Runtime.getRuntime().exec("java -jar " + jarPath.substring(1));
        System.exit(0);
    }
}
