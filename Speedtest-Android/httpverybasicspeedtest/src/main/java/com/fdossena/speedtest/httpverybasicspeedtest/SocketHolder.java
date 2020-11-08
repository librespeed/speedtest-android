package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

abstract class SocketHolder implements Runnable
{
    SpeedTestListener log;
    Socket socket;
    String host;
    int port;
    String path;
    SocketHolder(SpeedTestListener log, String host, int port, String path) throws IOException
    {
        this.log = log;
        SocketFactory factory = SocketFactory.getDefault();
        socket = factory.createSocket();
        socket.connect(new InetSocketAddress(host, port)); //"170.238.84.8", 8080
        this.host = host;
        this.port = port;
        this.path = path;
    }
}
