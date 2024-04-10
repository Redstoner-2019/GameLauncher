package me.redstoner2019;

import me.redstoner2019.gamelauncher.client.Client;
import me.redstoner2019.gamelauncher.server.Server;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length == 1){
            if(args[0].equalsIgnoreCase("server")) {
                Server.main(args);
                return;
            }
        }
        Client.main(args);
    }
}
