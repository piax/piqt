package org.piax.pubsub.stla;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.piax.pubsub.stla.LATopic;

public class LATopicTest {

    @Test
    public void test() {
        LATopic t1 = new LATopic("a");
        LATopic t2 = new LATopic("a");
        LATopic t3 = new LATopic("a");
        LATopic t4 = new LATopic("b");
        LATopic t5 = LATopic.topicMin("a");
        LATopic t6 = LATopic.topicMax("a");

        t1.setClusterId(new ClusterId("jp.jose.khn"));
        t2.setClusterId(new ClusterId("jp.jose.osk"));
        t3.setClusterId(new ClusterId("jp.jose.yrp"));
        t4.setClusterId(new ClusterId("jp.jose.osk"));

        t1.setId("01");
        t2.setId("02");
        t3.setId("03");
        t4.setId("04");

        LATopic t7 = LATopic.clusterMin(t3);
        LATopic t8 = LATopic.clusterMax(t3);
        LATopic t9 = LATopic.clusterMin(t6);

        ArrayList<LATopic> l = new ArrayList<LATopic>();
        l.add(t4);
        l.add(t2);
        l.add(t3);
        l.add(t1);
        l.add(t6);
        l.add(t5);
        l.add(t7);
        l.add(t8);
        l.add(t9);

        Collections.sort(l);

        assertTrue(l.get(0).equals(t5));
        assertTrue(l.get(5).equals(t6));
    }

}
