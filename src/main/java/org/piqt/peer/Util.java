/*
 * Util.java - A utility codes.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.peer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class
            .getPackage().getName());

    static public String newline = System.lineSeparator();

    public static String stackTraceStr(Exception e) {
        String ret = "";
        String msg = e.getMessage();
        if (msg != null) {
            ret += msg + newline;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        ret += sw.toString();
        return ret;
    }

    public static boolean isEmpty(String s) {
        if (s == null || s.equals("")) {
            return true;
        }
        return false;
    }

    public static boolean isOnlySpace(String s) {
        if (s == null)
            return false;
        if (s.trim().equals("")) {
            return true;
        }
        return false;
    }

    public static ArrayList<String> getIPs() {
        ArrayList<String> addrs = new ArrayList<String>();

        Enumeration<NetworkInterface> enuIfs;
        try {
            enuIfs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            String msg = "Failed to NetworkInterface.getNetworkInterfaces()";
            String detail = stackTraceStr(e);
            logger.error(msg + newline + detail);
            return addrs;
        }
        if (null != enuIfs) {
            while (enuIfs.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) enuIfs.nextElement();
                Enumeration<InetAddress> enuAddrs = ni.getInetAddresses();
                while (enuAddrs.hasMoreElements()) {
                    InetAddress in = (InetAddress) enuAddrs.nextElement();
                    addrs.add(in.getHostAddress());
                }
            }
        }
        return addrs;
    }
}
