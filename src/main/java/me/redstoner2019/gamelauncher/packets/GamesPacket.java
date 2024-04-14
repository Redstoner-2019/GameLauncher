package me.redstoner2019.gamelauncher.packets;

import me.redstoner2019.serverhandling.Packet;

import java.util.List;

public class GamesPacket extends Packet {
    private String gamesJSON;

    public String getGamesJSON() {
        return gamesJSON;
    }

    public void setGamesJSON(String gamesJSON) {
        this.gamesJSON = gamesJSON;
    }

    public GamesPacket(String gamesJSON) {
        this.gamesJSON = gamesJSON;
    }
}
