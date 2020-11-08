package com.fdossena.speedtest.httpverybasicspeedtest;

class DownUpSpeedResult
{
    double speed;
    double percentage;
    boolean ended;
    Throwable error;
    public DownUpSpeedResult(double speed, Throwable error,double percentage, boolean ended)
    {
        this.speed = speed;
        this.error = error;
        this.ended = ended;
        this.percentage = percentage;
    }
    public String toString()
    {
            return  "Ended = " + ended +
                    " Percentage = " + percentage +
                    " Speed  = " + speed +
                    " Error = " + (error==null?"None":error.getMessage());
    }
}
