package com.playip.speedtest.examplespeedtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fdossena.speedtest.core.serverSelector.TestPoint;

public class MainActivity extends AppCompatActivity implements ExampleSpeedTest.SpeedTestLog
{
    Button button_run;
    TextView tvReport;
    ExampleSpeedTest exampleSpeedTest;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        exampleSpeedTest = new ExampleSpeedTest();
        setContentView(R.layout.activity_main);
        button_run = ((Button)findViewById(R.id.button_run));
        tvReport = ((TextView)findViewById(R.id.tv_report));
        tvReport.setMovementMethod(new ScrollingMovementMethod());
        button_run.setOnClickListener
                (
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                new Thread(new TaskSpeed()).start();
                            }
                        }
                );

    }
    @Override
    public void speedTestlog(final String s)
    {

        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler .post
                (
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                tvReport.append(s+"\n");
                                System.out.println(s);
                            }
                        }
                );
    }
    private class TaskSpeed implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                String  name = "Helsinki, Finland";
                String server = "//fi.openspeed.org";
                String dlURL = "garbage.php";
                String ulURL = "empty.php";
                String pingURL = "empty.php";
                String getIpURL = "getIP.php";
                TestPoint tp = new TestPoint(name, server, dlURL, ulURL, pingURL, getIpURL);
                exampleSpeedTest.test(tp, MainActivity.this);
            }
            catch (Throwable e)
            {
            }
        }
    }

}
