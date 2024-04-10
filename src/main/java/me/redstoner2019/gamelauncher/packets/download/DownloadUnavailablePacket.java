package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class DownloadUnavailablePacket extends Packet {
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public DownloadUnavailablePacket(){

    }
    public DownloadUnavailablePacket(String reason) {
        this.reason = reason;
    }
}
