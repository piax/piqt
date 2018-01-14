/*
 * OnePeerMoquette.java - A sample.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.test;

import static org.piqt.web.MqttPiaxConfig.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.piax.ayame.ov.suzaku.SuzakuStrategy;
import org.piax.common.Destination;
import org.piax.gtrans.Peer;
import org.piax.gtrans.ov.suzaku.Suzaku;
import org.piax.pubsub.MqException;
import org.piax.pubsub.stla.LATKey;
import org.piqt.peer.PeerMqEngineMoquette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnePeerMoquette implements Runnable {

    static Logger logger = LoggerFactory.getLogger(OnePeerMoquette.class);

    static String configFile;
    Properties prop;
    Peer p;
    PeerMqEngineMoquette e;
    // piax only
    // PeerMqEngine e;
    Suzaku<Destination, LATKey> szk;

    public void init(String path) throws IOException {
        InputStream is = new FileInputStream(new File(path));
        prop = new Properties();
        prop.load(is);
        is.close();
        System.out.println("@@@ prop=" + prop);

        /*String LOGGING_PROPERTIES_DATA = "handlers=java.util.logging.ConsoleHandler, java.util.logging.FileHandler"
                + System.lineSeparator()
                + ".level=ALL"
                + System.lineSeparator()
                + "java.util.logging.ConsoleHandler.level=ALL"
                + System.lineSeparator()
                + "java.util.logging.ConsoleHandler.filter=org.piax.util.LogFilter"
                + System.lineSeparator()
                + "java.util.logging.ConsoleHandler.formatter=org.piax.util.LogFormatter"
                + System.lineSeparator()
                + "java.util.logging.FileHandler.level=ALL"
                + System.lineSeparator()
                + "java.util.logging.FileHandler.filter=org.piax.util.LogFilter"
                + System.lineSeparator()
                + "java.util.logging.FileHandler.formatter=org.piax.util.LogFormatter"
                + System.lineSeparator()
                + "java.util.logging.FileHandler.limit=1048576"
                + System.lineSeparator()
                + "java.util.logging.FileHandler.count=5"
                + System.lineSeparator()
                + "java.util.logging.FileHandler.pattern="
                + prop.getProperty(KEY_LOG_DESTINATION)
                + File.separator
                + "mqttpiax.log"
                + System.lineSeparator()
                + ".loglevel="
                + prop.getProperty(KEY_LOG_LEVEL) + System.lineSeparator();

        InputStream inS = null;
        inS = new ByteArrayInputStream(
                LOGGING_PROPERTIES_DATA.getBytes("UTF-8"));
        LogManager.getLogManager().readConfiguration(inS);
        inS.close();
        */
    }

    public void run() {
        try {
            e = new PeerMqEngineMoquette(prop.getProperty(KEY_PIAX_IP_ADDRESS),
                    Integer.valueOf(prop.getProperty(KEY_PIAX_PORT)),
                    toMQTTProps());
            // piax only
            // e = new PeerMqEngine(szk);
        } catch (MqException e1) {
            System.err.println("Error 3");
            e1.printStackTrace();
        }
        e.setSeed(prop.getProperty(KEY_PIAX_SEED_IP_ADDRESS),
                Integer.valueOf(prop.getProperty(KEY_PIAX_SEED_PORT)));
        e.setClusterId("");
        try {
            e.connect();
        } catch (MqException e1) {
            System.err.println("Error 4");
            e1.printStackTrace();
        }
        System.out.println(Thread.currentThread() + ":" + e.getPeerId() + " connected.");

        SuzakuStrategy.UPDATE_FINGER_PERIOD.set(5 * 1000);

        // piax only
        // if (prop.getProperty(KEY_PIAX_PEER_ID).equals("p1")) {
        // try {
        // e.subscribe("hello");
        // } catch (MqException e1) {
        // System.err.println("Error 5");
        // e1.printStackTrace();
        // }
        // System.out.println("@@@ p1 subscribed.");
        //
        // System.out.println("--------- WAIT std in------------");
        // try {
        // System.in.read();
        // } catch (IOException e1) {
        // System.err.println("Error 10");
        // e1.printStackTrace();
        // }
        // System.out.println("running fin");
        //
        // } else {
        // try {
        // Thread.sleep(3000);
        // } catch (InterruptedException e1) {
        // System.err.println("Error 6");
        // e1.printStackTrace();
        // }
        // long start = System.currentTimeMillis();
        // try {
        // e.publish("hello", ("" + start).getBytes(), 0);
        // } catch (MqException e1) {
        // System.err.println("Error 7");
        // e1.printStackTrace();
        // }
        // System.out.println("@@@ " + cid + " published.");
        // }
        // try {
        // Thread.sleep(3000);
        // fin();
        // } catch (InterruptedException e1) {
        // System.err.println("Error 8");
        // e1.printStackTrace();
        // } catch (MqException e1) {
        // System.err.println("Error 9");
        // e1.printStackTrace();
        // }
        // System.out.println("e: " + cid + " end.");
    }

    public void fin() throws MqException {
        e.disconnect();
        e.fin();
    }

    private Properties toMQTTProps() {
        Properties ret = new Properties();
        ret.setProperty("host", prop.getProperty(KEY_MQTT_BIND_ADDRESS));
        ret.setProperty("port", prop.getProperty(KEY_MQTT_PORT));
        ret.setProperty("persistent_store",
                prop.getProperty(KEY_MQTT_PERSISTENT_STORE));
        ret.setProperty("allow_anonymous",
                prop.getProperty(KEY_MQTT_ALLOW_ANONYMOUS));
        // The following line is TODO
        ret.setProperty("password_file", "");
        ret.setProperty("ssl_port", "8883");
        ret.setProperty("jks_path", "serverkeystore.jks");
        ret.setProperty("key_store_password", "passw0rdsrv");
        ret.setProperty("key_manager_password", "passw0rdsrv");
        ret.setProperty("authenticator_class", "");
        ret.setProperty("authorizator_class", "");
        return ret;
    }

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
                    configFile = param;
                } else {
                    System.err.println("-cDir option need parameter.");
                    return false;
                }
                i++;
            } else {
                System.err.println("Unknown param: " + args[i]);
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {

        parseCommandLine(args);
        OnePeerMoquette opm = new OnePeerMoquette();
        opm = new OnePeerMoquette();
        opm.init(configFile);
        opm.run();

        System.out.println("--------- WAIT std in------------");
        System.in.read();

        System.out.println("running fin");

        opm.fin();
        Thread.sleep(1000);
        System.out.println("end.");
        System.exit(0);
    }

}
