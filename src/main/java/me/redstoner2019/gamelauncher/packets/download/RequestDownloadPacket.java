package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class RequestDownloadPacket extends Packet {
    private String game;
    private String version;

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public RequestDownloadPacket(String game, String version) {
        this.game = game;
        this.version = version;
    }
}
