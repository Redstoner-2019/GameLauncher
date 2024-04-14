package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class ACK extends Packet {
    private long sum;

    public long getSum() {
        return sum;
    }

    public void setSum(long sum) {
        this.sum = sum;
    }

    public ACK(long sum) {
        this.sum = sum;
    }
}
