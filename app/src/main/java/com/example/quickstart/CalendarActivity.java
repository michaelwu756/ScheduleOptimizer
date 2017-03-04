package com.example.quickstart;

/**
 * Created by chees_000 on 3/4/2017.
 */

public class CalendarActivity {
    private String name;
    private int hours;

    public CalendarActivity(String n, int h)
    {
        name=n;
        hours=h;
    }

    public String getName()
    {
        return name;
    }

    public int getHours()
    {
        return hours;
    }

    public String toString()
    {
        return getName()+"\nHours: "+Integer.toString(getHours());
    }
}
