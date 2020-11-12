package com.fdossena.speedtest.httpverybasicspeedtest;

public interface SpeedTestListener
{
    //void speedTestProgress(); // if desired, the listener can ask for the current test results in this opportunity
    void speedTestEnded();
}
