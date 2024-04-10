package me.redstoner2019.gamelauncher.packets.download;

import me.redstoner2019.serverhandling.Packet;

public class DataPacket extends Packet {
    private int id;
    private int sum;
    private byte[] data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public DataPacket(int id, byte[] data) {
        this.id = id;
        this.data = data;
        for(byte b : data){
            this.sum+=b;
        }
    }
}
