package me.redstoner2019.gamelauncher.packets;

import me.redstoner2019.serverhandling.Packet;

public class JSONRequest extends Packet {
    private String json;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public JSONRequest(String json) {
        this.json = json;
    }
}
