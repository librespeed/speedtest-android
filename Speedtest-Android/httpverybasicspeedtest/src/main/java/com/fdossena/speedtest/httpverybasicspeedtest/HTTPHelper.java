package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class HTTPHelper
{
    static void putPostHeadersInStream(OutputStream out, String host, String path, int length) throws UnsupportedEncodingException
    {
        PrintStream ps = new PrintStream(out, false, "utf-8");
        ps.print("POST "+path+" HTTP/1.1\r\n"); // /backend/empty.php
        ps.print("Host: "+host+"\r\n"); // 192.168.0.102:8080
        ps.print("Connection: keep-alive\r\n");
        ps.print("Accept-Encoding: identity\r\n");
        ps.print("Content-Type: application/octet-stream\r\n");
        ps.print("Content-Length: " + length + "\r\n");
        ps.print("\r\n");
        ps.flush();
    }
    static void putGetHeadersInStream(OutputStream out, String host, String path) throws UnsupportedEncodingException
    {
        PrintStream ps = new PrintStream(out, false, "utf-8");
        ps.print("GET "+path+" HTTP/1.1\r\n");
        ps.print("Host: "+host+"\r\n");
        ps.print("Connection: keep-alive\r\n");
        ps.print("Accept-Encoding: identity\r\n");
        ps.print("\r\n");
        ps.flush();
    }
    public static class HTTPLineReader
    {
        InputStreamReader isr;
        HTTPLineReader(InputStream in) throws UnsupportedEncodingException
        {
            isr = new InputStreamReader(in, "utf-8");
        }
        String readLine() throws IOException
        {
            StringBuilder sb = new StringBuilder();
            while (true)
            {
                int c = isr.read();
                if (c == -1) break;
                sb.append((char) c);
                if (c == '\n') break;
            }
            return sb.toString();
        }
    }
}
