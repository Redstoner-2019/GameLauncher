package me.redstoner2019.gamelauncher.packets;

import me.redstoner2019.serverhandling.Packet;

import java.util.ArrayList;
import java.util.List;

public class VersionsPacket extends Packet {
    private List<String> versions = new ArrayList<>();

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public VersionsPacket(List<String> versions) {
        this.versions = versions;
    }
}
