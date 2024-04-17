package me.redstoner2019.gamelauncher.packets.server;

import me.redstoner2019.serverhandling.Packet;

public class ConsoleLine extends Packet {
    private String line;

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public ConsoleLine(String line) {
        this.line = line;
    }
}
