package com.example.quickstart;

/**
 * Created by chees_000 on 3/4/2017.
 */

public class CalendarEvent {
    private String name;
    private long startTime;
    private long endTime;

    public CalendarEvent(String n, long s, long e) {
        name = n;
        startTime = s;
        endTime = e;
    }

    public String getName() {
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }
}
