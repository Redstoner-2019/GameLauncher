package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class DownloadHeader extends Packet {
    private long packets;
    private long bytes;
    private int bytesPerPacket;
    private String filename;

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

    public DownloadHeader(long packets, long bytes, int bytesPerPacket, String filename) {
        this.packets = packets;
        this.bytes = bytes;
        this.bytesPerPacket = bytesPerPacket;
        this.filename = filename;
    }
}
