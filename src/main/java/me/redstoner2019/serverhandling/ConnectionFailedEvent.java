package me.redstoner2019.serverhandling;

public interface ConnectionFailedEvent {
    void onConnectionFailedEvent(Exception reason);
}
