package me.redstoner2019.gamelauncher.server;

public class GameServer {
    private String game;
    private boolean started = false;
    public GameServer(String game){
        this.game = game;
        this.started = false;
    }
    public void startServer(){
        if(started) return;

    }
}
