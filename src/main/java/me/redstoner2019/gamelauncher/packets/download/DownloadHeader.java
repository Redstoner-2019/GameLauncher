package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class DownloadHeader extends Packet {
    private long packets;
    private long bytes;
    private int bytesPerPacket;
    private String filename;
    private String game;
    private String version;
    private String json;
    private String fileType;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getPackets() {
        return packets;
    }

    public void setPackets(long packets) {
        this.packets = packets;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public int getBytesPerPacket() {
        return bytesPerPacket;
    }

    public void setBytesPerPacket(int bytesPerPacket) {
        this.bytesPerPacket = bytesPerPacket;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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

    public DownloadHeader(long packets, long bytes, int bytesPerPacket, String filename, String game, String version, String fileType) {
        this.packets = packets;
        this.bytes = bytes;
        this.bytesPerPacket = bytesPerPacket;
        this.filename = filename;
        this.game = game;
        this.version = version;
        this.fileType = fileType;
    }
}
