package me.redstoner2019.serverhandling;

public interface ClientConnectEvent {
    void connectEvent(ClientHandler handler) throws Exception;
}
