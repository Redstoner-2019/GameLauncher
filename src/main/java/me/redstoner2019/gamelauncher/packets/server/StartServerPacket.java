package me.redstoner2019.gamelauncher.packets.server;

import me.redstoner2019.serverhandling.Packet;

public class StartServerPacket extends Packet {
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

    public StartServerPacket(String game, String version) {
        this.game = game;
        this.version = version;
    }
}
