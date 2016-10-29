package org.piax.pubsub.stla;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class ClusterIdTest {

    @Test
    public void test() {
        assertTrue(new ClusterId("jp.jose.yrp").toString().equals("jp.jose.yrp"));
        assertTrue(new ClusterId(new String[] { "jp", "jose" }).toString().equals("jp.jose"));
        assertTrue(new ClusterId(new String[] { "" }).toString().equals(""));
        assertTrue(new ClusterId("jp.jose.yrp").distance(new ClusterId("jp.jose.osk")) == 1);
        assertTrue(new ClusterId("jp.jose.yrp").distance(new ClusterId("jp.isp1.osk")) == 2);
        assertTrue(new ClusterId("jp").distance(new ClusterId("jp.isp1.osk")) == 2);
        assertTrue(new ClusterId("").distance(new ClusterId("jp.isp1.osk")) == 3);
        assertTrue(new ClusterId("jp.isp1.osk").distance(new ClusterId("jp.isp1.osk")) == 0);
        assertTrue(new ClusterId("jp.isp1.osk").compareTo(new ClusterId("jp")) == 1);
        ClusterId[] cis = new ClusterId[] { new ClusterId("jp.isp1.osk"),
                new ClusterId("jp"), new ClusterId("jp.jose.yrp"),
                new ClusterId("jp.jose.yrp1"), new ClusterId("jp.jose.yrp2"),
                new ClusterId("jp.isp1"), new ClusterId("jp"), };
        for (int i = 0; i < 100000; i++) {
            Arrays.sort(cis);
        }
        assertTrue(cis[0].toString().equals("jp"));;
        assertTrue(cis[6].toString().equals("jp.jose.yrp2"));;
    }

}
