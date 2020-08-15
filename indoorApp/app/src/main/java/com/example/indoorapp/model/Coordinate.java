package com.example.indoorapp.model;

import io.realm.RealmObject;

public class Coordinate extends RealmObject {
    private String bssid;
    private String signal_Strenght;
    private String position;


    public Coordinate() {
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Coordinate(String signal_Strenght, String bssid) {
        this.signal_Strenght = signal_Strenght;
        this.bssid = bssid;
        this.position=position;
    }

    public String getSignal_Strenght() {
        return signal_Strenght;
    }

    public void setSignal_Strenght(String signal_Strenght) {
        this.signal_Strenght = signal_Strenght;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "bssid='" + bssid + '\'' +
                ", signal_Strenght='" + signal_Strenght + '\'' +
                ", position='" + position + '\'' +
                '}';
    }
}
