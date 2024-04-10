package me.redstoner2019.serverhandling;

public interface ConnectionLostEvent {
    void onConnectionLostEvent(String reason);
}
