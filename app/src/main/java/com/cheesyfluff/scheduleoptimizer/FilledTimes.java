package com.cheesyfluff.scheduleoptimizer;

public class FilledTimes {
    private long startTime;
    private long endTime;

    public FilledTimes(long s, long e)
    {
        startTime=s;
        endTime=e;
    }

    public long getStart()
    {
        return startTime;
    }

    public long getEnd()
    {
        return endTime;
    }
}
