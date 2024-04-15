package me.redstoner2019.configuration;

import java.util.ArrayList;
import java.util.List;

public class Version {
    public String name;
    public List<Type> types = new ArrayList<>();

    public void addType(Type type) {
        types.add(type);
    }
    public boolean hasType(String type){
        for(Type t : types) if(t.name.equals(type)) return true;
        return false;
    }
    public Type getType(String name){
        for(Type o : types) if(o.name.equals(name)) return o;
        return null;
    }

    public Version(String name) {
        this.name = name;
    }
}
