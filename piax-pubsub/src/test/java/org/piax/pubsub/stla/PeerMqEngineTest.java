package org.piax.pubsub.stla;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.piax.pubsub.MqCallback;
import org.piax.pubsub.MqDeliveryToken;
import org.piax.pubsub.MqMessage;
import org.piax.pubsub.MqTopic;

public class PeerMqEngineTest {

    @Test
    public void SinglePeerTest() {
        try {
            AtomicInteger count = new AtomicInteger();
            PeerMqEngine engine = new PeerMqEngine("localhost", 12367);
            engine.setCallback(new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                //    System.out.println("received:" + m + " on subscription:"
                //            + subscribedTopic.getSpecified() + " for topic:"
                //            + m.getTopic());
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
              //      System.out.println("delivered:"
              //              + token.getMessage().getTopic());
                }
            });
            engine.setSeed("localhost", 12367);
            // engine.setClusterId("cluster.test");
            engine.connect();
            engine.subscribe("sport/tennis/player1");
            //System.out.println("joinedKeys=" + engine.getJoinedKeys());
            engine.publish("sport/tennis/player1", "hello1".getBytes(), 0);
            engine.subscribe("#");
            engine.subscribe("+/#");
            engine.subscribe("/+/#");
            engine.subscribe("sport/+");
            //System.out.println("joinedKeys=" + engine.getJoinedKeys());
            //System.out.println("sleeping 20 sec");
            Thread.sleep(200);
            count.set(0);
            engine.publish("sport/tennis", "hello2".getBytes(), 0);
            assertTrue(count.get() == 3);
            count.set(0);
            engine.publish("/sport/tennis", "hello3".getBytes(), 1);
            assertTrue(count.get() == 3);
            engine.disconnect();
            engine.fin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void MultiplePeersWithDelegateTest() {
        PeerMqDeliveryToken.USE_DELEGATE = true;
        MultiplePeersRun();
    }
    
    @Test
    public void MultiplePeersWithoutDelegateTest() {
        PeerMqDeliveryToken.USE_DELEGATE = false;
        MultiplePeersRun();
    }
    
    public void MultiplePeersRun() {
        try {
            AtomicInteger count = new AtomicInteger();
            PeerMqEngine engine1 = new PeerMqEngine("localhost", 12367);
            PeerMqEngine engine2 = new PeerMqEngine("localhost", 12368);
            PeerMqEngine engine3 = new PeerMqEngine("localhost", 12369);
            MqCallback cb1 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                   // System.out.println("received:" + m + " on subscription:"
                   //         + subscribedTopic.getSpecified() + " for topic:"
                   //         + m.getTopic() + " on engine1");
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    //System.out.println("delivered:"
                    //        + token.getMessage().getTopic());
                }
            };
            MqCallback cb2 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                   // System.out.println("received:" + m + " on subscription:"
                   //         + subscribedTopic.getSpecified() + " for topic:"
                   //         + m.getTopic() + " on engine2");
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    //System.out.println("delivered:"
                    //        + token.getMessage().getTopic());
                }
            };
            MqCallback cb3 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                   // System.out.println("received:" + m + " on subscription:"
                   //         + subscribedTopic.getSpecified() + " for topic:"
                   //         + m.getTopic() + " on engine3");
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    //System.out.println("delivered:"
                    //        + token.getMessage().getTopic());
                }
            };
            engine1.setCallback(cb1);
            engine2.setCallback(cb2);
            engine3.setCallback(cb3);
            
            engine1.setSeed("localhost", 12367);
            engine2.setSeed("localhost", 12367);
            engine3.setSeed("localhost", 12367);
            // engine.setClusterId("cluster.test");
            engine1.connect();
            engine2.connect();
            engine3.connect();
            
            engine2.subscribe("sport/tennis/player1");
            
            //System.out.println("joinedKeys=" + engine.getJoinedKeys());
            engine1.publish("sport/tennis/player1", "hello1".getBytes(), 0);
            
            engine3.subscribe("#");
            engine1.subscribe("+/#");
            engine2.subscribe("/+/#");
            engine1.subscribe("sport/+");
            //System.out.println("joinedKeys=" + engine.getJoinedKeys());
            //System.out.println("sleeping 20 sec");
            Thread.sleep(2000);
            count.set(0);
            engine1.publish("sport/tennis", "hello2".getBytes(), 0);
            Thread.sleep(2000);
            assertTrue(count.get() == 3);
            //System.out.println("count=" + count.get());
            count.set(0);
            engine1.publish("/sport/tennis", "hello3".getBytes(), 1);
            Thread.sleep(2000);
            assertTrue(count.get() == 3);
            //System.out.println("count=" + count.get());
            engine1.disconnect();
            engine2.disconnect();
            engine3.disconnect();
            engine1.fin();
            engine2.fin();
            engine3.fin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void UserMigrationTest() {
        try {
            AtomicInteger count = new AtomicInteger();
            PeerMqEngine engine1 = new PeerMqEngine("localhost", 12367);
            PeerMqEngine engine2 = new PeerMqEngine("localhost", 12368);
            PeerMqEngine engine3 = new PeerMqEngine("localhost", 12369);
            MqCallback cb1 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                    //System.out.println("received:" + m + " on subscription:"
                    //        + subscribedTopic.getSpecified() + " for topic:"
                    //        + m.getTopic() + " on engine1");
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    //System.out.println("delivered:"
                    //        + token.getMessage().getTopic());
                }
            };
            MqCallback cb2 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                    //System.out.println("received:" + m + " on subscription:"
                    //        + subscribedTopic.getSpecified() + " for topic:"
                    //        + m.getTopic() + " on engine2");
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    //System.out.println("delivered:"
                    //        + token.getMessage().getTopic());
                }
            };
            MqCallback cb3 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    count.incrementAndGet();
                   //System.out.println("received:" + m + " on subscription:"
                   //         + subscribedTopic.getSpecified() + " for topic:"
                   //         + m.getTopic() + " on engine3");
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    //System.out.println("delivered:"
                    //        + token.getMessage().getTopic());
                }
            };
            engine1.setCallback(cb1);
            engine2.setCallback(cb2);
            engine3.setCallback(cb3);
            PeerMqDeliveryToken.USE_DELEGATE = false;
            engine1.setSeed("localhost", 12367);
            engine2.setSeed("localhost", 12367);
            engine3.setSeed("localhost", 12367);
            // engine.setClusterId("cluster.test");
            engine1.connect();
            engine2.connect();
            engine3.connect();
            Thread.sleep(200);
            engine1.subscribe("sport/tennis/player1");
            engine1.publish("sport/tennis/player1", "hello2".getBytes(), 0);
            engine2.subscribe("sport/tennis/player1");
            engine1.unsubscribe("sport/tennis/player1");
            engine2.publish("sport/tennis/player1", "hello3".getBytes(), 0);
            engine2.unsubscribe("sport/tennis/player1");
            engine3.subscribe("sport/tennis/player1");
            engine2.publish("sport/tennis/player1", "hello4".getBytes(), 0);
            Thread.sleep(1000);
            assertTrue(count.get() == 3);
            engine1.disconnect();
            engine2.disconnect();
            engine3.disconnect();
            engine1.fin();
            engine2.fin();
            engine3.fin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    
    @Test
    public void UnsubscribeTest() {
        try {
            PeerMqEngine engine1 = new PeerMqEngine("localhost", 12367);
            MqCallback cb1 = new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    System.out.println(new String(m.getPayload()));
                }
                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                }
            };
            engine1.setCallback(cb1);
            PeerMqDeliveryToken.USE_DELEGATE = false;
            engine1.setSeed("localhost", 12367);
            engine1.connect();
            Thread.sleep(200);
            engine1.subscribe("#");
            int size = engine1.o.getKeys().size();
            engine1.publish("sport/tennis/player1", "hello2".getBytes(), 0);
            engine1.unsubscribe("#");
            assertTrue(engine1.subscribes.size() == 0);
            assertFalse(engine1.o.getKeys().size() == size);
//            Thread.sleep(500);
//            engine1.publish("sport/tennis/player1", "hello2".getBytes(), 0);
//            Thread.sleep(2000);
            engine1.disconnect();
            engine1.fin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
}
