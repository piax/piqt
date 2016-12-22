/*
 * Launcher.java - The startup class
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.web;

import static org.piqt.peer.Util.isEmpty;
import static org.piqt.peer.Util.newline;
import static org.piqt.peer.Util.stackTraceStr;
import static org.piqt.web.MqttPiaxConfig.KEY_AUTO_START;
import static org.piqt.web.MqttPiaxConfig.KEY_CLEAR_START;
import static org.piqt.web.MqttPiaxConfig.KEY_JETTY_HOST;
import static org.piqt.web.MqttPiaxConfig.KEY_JETTY_PORT;
import static org.piqt.web.MqttPiaxConfig.KEY_MQTT_PERSISTENT_STORE;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_DOMAIN_NAME;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_IP_ADDRESS;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_PORT;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_SEED_IP_ADDRESS;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_SEED_PORT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.arnx.jsonic.JSON;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.security.HashLoginService;
import org.piax.pubsub.MqException;
import org.piax.pubsub.stla.PeerMqDeliveryToken;
import org.piqt.peer.PeerMqEngineMoquette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class
            .getPackage().getName());
    private static Launcher instance;
    private static boolean cont = true;

    public static enum response {
        OK, NG
    };

    public static String PROPERTY_FILE_NAME = "config/piqt.properties";
    public static String LOG_FILE_NAME = "piqt.log";
    public static String PIQT_PATH_PROPERTY_NAME = "piqt.path";

    Server server;
    MqttPiaxConfig config;
    PeerMqEngineMoquette e;

    long startDate;

    public static String version() {
        return Launcher.class.getPackage().getImplementationVersion();
    }

    public synchronized static Launcher getInstance() {
        if (instance == null) {
            instance = new Launcher();
        }
        return instance;
    }

    class Jdata {
        public String msg;
        public String detail;

        Jdata(String m, String d) {
            msg = m;
            detail = d;
        }
    }

    private Response buildResponse(response status, String msg, String detail) {
        ResponseBuilder b;

        if (status == response.OK) {
            b = Response.ok();
        } else {
            b = Response.serverError();
        }
        b.type(MediaType.APPLICATION_JSON);
        b.entity(JSON.encode(new Jdata(msg, detail)));
        return b.build();

    }

    private void readConfig(File configFile) throws IOException {
        if (config == null) {
            InputStream is = new FileInputStream(configFile);
            MqttOnPiaxApp.configuration = new Properties();
            MqttOnPiaxApp.configuration.load(is);
            is.close();
            config = new MqttPiaxConfig(MqttOnPiaxApp.configuration);
        }
    }

    /* XXX not used?
    private String escapeSeparators(String str) {
        return str.replace("\\", "\\\\").replace("#", "\\#")
                .replace("!", "\\!").replace("=", "\\=").replace(":", "\\:");
    } */

    private boolean parseCommandLine(String args[]) {
        if (args.length == 1 && args[0].equals("-version")) {
            System.out.println(version());
            return false;
        }
        return true;
    }

    private void checkDomain() {
        String domain = (String) config.get(KEY_PIAX_DOMAIN_NAME);
        if (isEmpty(domain)) {
            // The default cluster ID is empty string.
            domain = "";
            config.setConfig(KEY_PIAX_DOMAIN_NAME, domain);
            logger.info("domain name is set as " + (isEmpty(domain) ? "default" : domain));
            MqttOnPiaxApp.configuration.setProperty(KEY_PIAX_DOMAIN_NAME,
                    domain);
        }
    }
    
    private File configFile() {
        return new File(System.getProperty(PIQT_PATH_PROPERTY_NAME), PROPERTY_FILE_NAME);
    }

    private void init(String args[]) {
        server = null;

        if (!parseCommandLine(args)) {
            System.exit(1);
        }

        MqttOnPiaxApp.setLauncher(this);

        try {
            readConfig(configFile());
        } catch (IOException e1) {
            String msg = "Failed to read configuration file: ";
            String detail = stackTraceStr(e1);
            logger.error(msg + detail);
            System.exit(1);
        }

        checkDomain();

        server = new Server();
        ServerConnector http = new ServerConnector(server);
        String jh = (String) config.get(KEY_JETTY_HOST);
        int jp = (int) config.get(KEY_JETTY_PORT);
        if (isEmpty(jh) || jp == 0) {
            logger.error("Wrong jetty host or jetty port.");
            System.exit(1);
        }
        http.setHost(jh);
        http.setPort(jp);
        http.setIdleTimeout(30000);

        server.addConnector(http);

        /**/
        ProtectionDomain protectionDomain = Launcher.class
                .getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        logger.debug("protectionDomain=" + location.toExternalForm());

        WebAppContext wap = new WebAppContext();
        wap.setContextPath("/");
        wap.setParentLoaderPriority(false);

        String rb = System.getProperty(PIQT_PATH_PROPERTY_NAME) + File.separator + "WebContent";
        logger.info("content path: " + rb);
        wap.setResourceBase(rb);
        
        HashLoginService loginService = new HashLoginService("PIQTRealm");
        loginService.setConfig(rb + File.separator + "piqtrealm.txt");
        server.addBean(loginService);

        server.setHandler(wap);
        /**/

        try {
            server.start();
        } catch (Exception e1) {
            String msg = "Failed to setLogger().";
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            System.exit(1);
        }

        startDate = 0;

        e = null;

        if (config.get(KEY_AUTO_START).equals("yes")) {
            Response res = this.start();
            if (res.getStatusInfo() != Response.Status.OK) {
                logger.error((String) res.getEntity());
            }
        }
    }

/*
    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (Exception e) {
            String msg = "Failed to getHostName().";
            String detail = stackTraceStr(e);
            logger.error(msg + newline + detail);
        }
        return "UnknownHost";
    }
*/
    public Response start() {
        String startLog = "";
        if (startDate != 0) {
            String msg = "MQTT already started.";
            logger.info(msg);
            return buildResponse(response.OK, msg, String.valueOf(startDate));
        }

        if (config.get(KEY_CLEAR_START).equals("yes")) {
            String store = (String) config.get(KEY_MQTT_PERSISTENT_STORE);
            File f = new File(store);
            if (f.exists()) {
                f.delete();
                logger.info("persistent store " + store + " removed.");
            } else {
                logger.info("persistent store " + store + " does not exist.");
            }
        }

        PeerMqDeliveryToken.USE_DELEGATE = true;
        /* NodeMonitor.PING_TIMEOUT = 1000000; // to test the retrans without ddll
                                            // fix
        RQManager.RQ_FLUSH_PERIOD = 50; // the period for flushing partial
                                        // results in intermediate nodes
        RQManager.RQ_EXPIRATION_GRACE = 80; // additional grace time before
                                            // removing RQReturn in intermediate
                                            // nodes
        RQManager.RQ_RETRANS_PERIOD = 1000; // range query retransmission period
        MessagingFramework.ACK_TIMEOUT_THRES = 2000;
        MessagingFramework.ACK_TIMEOUT_TIMER = MessagingFramework.ACK_TIMEOUT_THRES + 50; */

        boolean bad_param = false;
        String msg = "";
        String pip = (String) config.get(KEY_PIAX_IP_ADDRESS);
        int pport = (int) (config.get(KEY_PIAX_PORT));
        //String pid = (String) (config.get(KEY_PIAX_PEER_ID));
        String sip = (String) config.get(KEY_PIAX_SEED_IP_ADDRESS);
        int sport = (int) (config.get(KEY_PIAX_SEED_PORT));
        if (isEmpty(pip)) {
            msg = "Wrong peer ip address.";
            bad_param = true;
        }
        if (pport == 0) {
            msg = "Wrong peer port.";
            bad_param = true;
        }
        //if (isEmpty(pid)) {
        //    msg = "Wrong peer ID";
        //    bad_param = true;
        //}
        checkDomain();
        String pcid = (String) (config.get(KEY_PIAX_DOMAIN_NAME));
        if (sport == 0) {
            msg = "Wrong seed port.";
            bad_param = true;
        }
        if (bad_param) {
            logger.error(msg);
            return buildResponse(response.NG, msg, null);
        }
/*
        try {
            loc = new UdpLocator(new InetSocketAddress(pip, pport));
            peer = Peer.getInstance(PeerId.newId());
            startLog += "peer instance created." + newline;
            cid = new ClusterId(pcid);
        } catch (RuntimeException e1) {
            fin();
            msg = "";
            if (loc == null) {
                msg = startLog + "Failed to create locator.";
            } else if (peer == null) {
                msg = startLog + "Failed to create peer.";
            } else if (cid == null) {
                msg = startLog + "Failed to get ClusterId.";
            }
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            return buildResponse(response.NG, msg, detail);
        }

        try {
            c = peer.newBaseChannelTransport((UdpLocator) loc);
            szk = new Suzaku<Destination, LATKey>(c);
        } catch (IdConflictException | IOException e1) {
            fin();
            msg = startLog + "Failed to start peer.";
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            return buildResponse(response.NG, msg, detail);
        }
        startLog += "Suzaku/";
*/
        try {
//            e = new PeerMqEngineMoquette(szk, config.toMQTTProps());
            e = new PeerMqEngineMoquette(pip, pport, config.toMQTTProps());
        } catch (IOException | MqException e1) {
            msg = startLog + "Failed to start moquette.";
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            return buildResponse(response.NG, msg, detail);
        }
        startLog += "MqEngine";
        e.setSeed(sip, sport);
        e.setClusterId(pcid);
        try {
            e.connect();
        } catch (MqException e1) {
            e.fin();
            msg = startLog + "peer failed to join().";
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            return buildResponse(response.NG, msg, detail);
        }
        startLog += "(pid=" + e.getPeerId() + "," + " cid=" + (e.getClusterId().isZeroLength()? "default" : e.getClusterId())
                + ") started.";
        startDate = new Date().getTime();
        logger.info(startLog);
        return buildResponse(response.OK, startLog, String.valueOf(startDate));
    }

    public Response stop() {
        if (startDate != 0) {
            startDate = 0;
        } else {
            String msg = "MQTT already stopped.";
            logger.info(msg);
            return buildResponse(response.OK, msg, null);
        }

        try {
            if (e != null) {
                e.disconnect();
                e.fin();
            } else {
                return buildResponse(response.OK, "MQTT already stopped.", "");
            }
        } catch (MqException e1) {
            String msg = "peer failed to disconnect() or fin().";
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            return buildResponse(response.NG, msg, detail);
        }
        logger.info("MQTT stopped.");
        return buildResponse(response.OK, "MQTT stopped.", "");
    }

    public Response saveConfig(LinkedHashMap<String, String> data) {
        config.setConfig(data);
        MqttOnPiaxApp.configuration = config.toProperties();
        logger.debug("MqttOnPiaxApp.configuration:"
                + MqttOnPiaxApp.configuration);

        FileOutputStream os;
        try {
            os = new FileOutputStream(configFile());
            MqttOnPiaxApp.configuration.store(os, "update props");
            os.close();
        } catch (IOException e1) {
            String msg = "Failed to store config in " + configFile() + ".";
            String detail = stackTraceStr(e1);
            logger.error(msg + newline + detail);
            return buildResponse(response.NG, msg, detail);
        }
        logger.info("Stored config in " + configFile() + ".");
        return buildResponse(response.OK, "saveConfig succeeded.", "");
    }

    public String getStatistics() {
        if (e == null)
            return null;
        else
            return e.getStatistics();
    }

    public long getStartDate() {
        return startDate;
    }

    private void jetty_stop() {
        if (server != null) {
            try {
                server.stop();
                server = null;
            } catch (Exception e) {
                String detail = stackTraceStr(e);
                logger.error("Failed to stop Jetty.\n" + detail);
            }
        }
    }

    public static void main(String[] args) {
        Launcher launcher = Launcher.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                launcher.jetty_stop();
                cont = false;
            }
        });
        launcher.init(args);
        while (cont) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                String msg = "sleep error at main. exit.";
                String detail = stackTraceStr(e1);
                logger.error(msg + newline + detail);
                System.exit(1);
            }
        }
        System.exit(0);
    }

}
