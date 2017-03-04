package com.example.quickstart;

/**
 * Created by chees_000 on 3/4/2017.
 */

public class FilledTimes {
    private String startTime;
    private String endTime;

    public FilledTimes(String s, String e)
    {
        startTime=s;
        endTime=e;
    }

    public String getStart()
    {
        return startTime;
    }

    public String getEnd()
    {
        return endTime;
    }
}
