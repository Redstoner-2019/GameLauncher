package me.redstoner2019.gamelauncher.packets;

import me.redstoner2019.serverhandling.Packet;

public class RequestGameVersions extends Packet {
    private String game;

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public RequestGameVersions(String game) {
        this.game = game;
    }
}
