package me.redstoner2019.configuration;

import java.util.ArrayList;
import java.util.List;

public class Game {
    public Game(String name) {
        this.name = name;
    }

    public String name;
    public List<Version> versions = new ArrayList<>();
    public void addVersion(Version version) {
        versions.add(version);
    }
    public boolean hasVersion(String version){
        for(Version v : versions) if(v.name.equals(version)) return true;
        return false;
    }
    public Version getVersion(String name){
        for(Version o : versions) if(o.name.equals(name)) return o;
        return null;
    }
}
