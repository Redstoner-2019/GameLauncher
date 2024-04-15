package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class RequestDownloadPacket extends Packet {
    private String game;
    private String version;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public RequestDownloadPacket(String game, String version, String type) {
        this.game = game;
        this.version = version;
        this.type = type;
    }
}
