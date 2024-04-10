package me.redstoner2019.gamelauncher.packets;

import me.redstoner2019.serverhandling.Packet;

import java.util.List;

public class GamesPacket extends Packet {
    private List<String> games;

    public List<String> getGames() {
        return games;
    }

    public void setGames(List<String> games) {
        this.games = games;
    }

    public GamesPacket(List<String> games) {
        this.games = games;
    }
}
