/*
 * ClusterId.java - The cluster id of STLA.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import java.io.Serializable;
import java.util.Arrays;

public class ClusterId implements Comparable<Object>, Serializable {
    private static final long serialVersionUID = -7993860373290119134L;
    String[] ids;

    public ClusterId(String fqci) {
        ids = fqci.split("\\.");
    }

    public ClusterId(String[] ids) {
        this.ids = ids;
    }

    public String[] getIdPath() {
        return ids;
    }

    public boolean isZeroLength() {
        return ids.length == 1 && ids[0].equals("");
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClusterId)) {
            return false;
        }
        return Arrays.equals(this.ids, ((ClusterId) o).getIdPath());
    }

    public int distance(Object o) {
        if (!(o instanceof ClusterId)) {
            return -1;
        }
        String[] oids = ((ClusterId) o).getIdPath();
        String[] longer;
        String[] shorter;
        if (oids.length > ids.length) {
            longer = oids;
            shorter = ids;
        } else {
            longer = ids;
            shorter = oids;
        }
        int count = 0;
        for (int i = 0; i < shorter.length; i++) {
            if (longer[i].equals(shorter[i])) {
                count++;
            }
        }
        return longer.length - count;
    }

    public String toString() {
        String ret = "";
        for (int i = 0; i < ids.length; i++) {
            ret += ids[i];
            if (i != ids.length - 1) {
                ret += ".";
            }
        }
        return ret;
    }

    @Override
    public int compareTo(Object o) {
        // return toString().compareTo(o.toString());
        // following is ten-times faster than above.
        if (!(o instanceof ClusterId)) {
            return -1;
        }
        String[] oids = ((ClusterId) o).getIdPath();
        for (int i = 0; i < ids.length; i++) {
            if (oids.length <= i) {
                return 1;
            }
            int ret = ids[i].compareTo(oids[i]);
            if (ret != 0) {
                return ret;
            }
        }
        // all element in ids is same as oids
        int ret = ids.length - oids.length;
        return ret;

    }

    public static void main(String[] args) {
        System.out.println("'" + new ClusterId("jp.jose.yrp") + "'");
        System.out.println("'" + new ClusterId(new String[] { "jp", "jose" })
                + "'");
        System.out.println("'" + new ClusterId(new String[] { "" }) + "'");
        System.out.println("'"
                + new ClusterId(new String[] { "jp", "jose", "yrp" }) + "'");
        System.out.println("'"
                + new ClusterId(new String[] { "jp", "isp", "gw1", "core1" })
                + "'");
        System.out.println(new ClusterId("jp.jose.yrp").distance(new ClusterId(
                "jp.jose.osk")));
        System.out.println(new ClusterId("jp.jose.yrp").distance(new ClusterId(
                "jp.isp1.osk")));
        System.out.println(new ClusterId("jp").distance(new ClusterId(
                "jp.isp1.osk")));
        System.out.println(new ClusterId("").distance(new ClusterId(
                "jp.isp1.osk")));
        System.out.println(new ClusterId("jp.isp1.osk").distance(new ClusterId(
                "jp.isp1.osk")));
        System.out.println(new ClusterId("jp.isp1.osk")
                .compareTo(new ClusterId("jp")));
        ClusterId[] cis = new ClusterId[] { new ClusterId("jp.isp1.osk"),
                new ClusterId("jp"), new ClusterId("jp.jose.yrp"),
                new ClusterId("jp.jose.yrp1"), new ClusterId("jp.jose.yrp2"),
                new ClusterId("jp.isp1"), new ClusterId("jp"), };
        long s = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            Arrays.sort(cis);
        }
        System.out.println("time=" + (System.currentTimeMillis() - s));
        System.out.println(Arrays.toString(cis));
    }

}
