package me.redstoner2019;

import me.redstoner2019.configuration.Configuration;
import me.redstoner2019.configuration.Game;
import me.redstoner2019.configuration.Type;
import me.redstoner2019.configuration.Version;
import me.redstoner2019.gamelauncher.client.Client;
import me.redstoner2019.gamelauncher.server.Server;

import java.io.File;

public class Main {
    public static int DEFAULT_SIZE = 131072;
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
}
