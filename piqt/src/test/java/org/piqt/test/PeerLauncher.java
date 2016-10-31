/*
 * PeerLauncher.java - A sample.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.test;

import java.util.ArrayList;

public class PeerLauncher extends Thread {
    static ArrayList<String> configFiles;

    private static boolean parseCommandLine(String args[]) {

        String param = null;
        for (int i = 0; i < args.length; i++) {
            if ((i + 1) >= args.length)
                param = null;
            else
                param = args[i + 1];

            if (args[i].equals("-c")) {
                if (param != null && param.charAt(0) != '-') {
                    ++i;
                    configFiles.add(param);
                } else {
                    System.err.println("-cDir option need parameter.");
                    return false;
                }
            } else {
                System.err.println("Unknown param: " + args[i]);
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {

        configFiles = new ArrayList<String>();
        parseCommandLine(args);
        OnePeerMoquette opm[] = new OnePeerMoquette[configFiles.size()];
        Thread th[] = new Thread[configFiles.size()];

        int i = 0;
        for (String path : configFiles) {
            opm[i] = new OnePeerMoquette();
            opm[i].init(path);
            th[i] = new Thread(opm[i]);
            th[i].start();
            i++;
            Thread.sleep(100);
        }

        System.out.println("--------- WAIT std in------------");
        System.in.read();

        System.out.println("running fin");

        for (--i; i >= 0; i--) {
            opm[i].fin();
        }

        Thread.sleep(2000);
        System.out.println("end.");
    }

}
