package com.cheesyfluff.scheduleoptimizer;

public class CalendarActivity {
    private String name;
    private int hours;
    private String days;

    public CalendarActivity(String n, int h, String d)
    {
        name=n;
        hours=h;
        days=d;
    }

    public String getName()
    {
        return name;
    }

    public int getHours()
    {
        return hours;
    }

    public String getDays()
    {
        return days;
    }

    public String toString()
    {
        return getName()+"\nHours: "+Integer.toString(getHours())+"\nSMTWTFS: "+getDays();
    }
}
