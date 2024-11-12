package com.example.captest;

public class CustomDomain {
    private String hour;
    private int temp;
    private String picPath;
    private int rain;


    public CustomDomain(String hour, int temp, int rain, String picPath) {
        this.hour = hour;
        this.temp = temp;
        this.picPath = picPath;
        this.rain = rain;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public int getTemp() {
        return temp;
    }

    public void setTemp(int temp) {
        this.temp = temp;
    }

    public int getRain() {
        return rain;
    }

    public void setRain(int rain) {
        this.rain = rain;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }
}