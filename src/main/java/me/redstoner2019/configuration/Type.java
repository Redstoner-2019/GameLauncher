package me.redstoner2019.configuration;

public class Type {
    public String name;
    public String file;
    public long size;
    public String changes;
    public String title;

    public Type(String name) {
        this.name = name;
        file = "";
        size = 0;
        title = "";
        changes = "";
    }
}
