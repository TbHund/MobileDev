package com.example.calculator;

public class WeatherData {
    private Location location;
    private Current current;

    public static class Location {
        String name;
        String country;
        public String getName() { return name; }
    }

    public static class Current {
        double temp_c;
        Condition condition;
        public double getTempC() { return temp_c; }
        public Condition getCondition() { return condition; }
    }

    public static class Condition {
        String text;
        String icon;
        public String getText() { return text; }
    }

    public Location getLocation() { return location; }
    public Current getCurrent() { return current; }
}