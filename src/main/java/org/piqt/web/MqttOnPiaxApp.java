/*
 * MqttOnPiaxApp.java - The web config for PIQT(PIAX/MQTT)
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.arnx.jsonic.JSON;

import org.piqt.peer.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class MqttOnPiaxApp {
    private static final Logger logger = LoggerFactory
            .getLogger(MqttOnPiaxApp.class.getPackage().getName());

    static HashMap<String, String> settings = new HashMap<>();
    static Properties configuration;
    static Launcher launcher;

    static void setLauncher(Launcher l) {
        launcher = l;
    }

    @GET
    @Path("/loadConfig")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadConfig() throws IOException {
        configuration.setProperty("start_time",
                String.valueOf(launcher.getStartDate()));
        String s = JSON.encode(configuration);
        logger.debug("Enter. Configuration: " + s);

        ResponseBuilder b = Response.ok();
        b.type(MediaType.APPLICATION_JSON);
        b.entity(s);
        return b.build();
    }

    @GET
    @Path("/loadIPs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadIPs() {
        ArrayList<String> addrs = Util.getIPs();
        String s = JSON.encode(addrs);
        logger.debug("Enter loadIPs: " + s);

        ResponseBuilder b = Response.ok();
        b.type(MediaType.APPLICATION_JSON);
        b.entity(s);
        return b.build();
    }

    @POST
    @Path("/saveConfig")
    public Response applyConfig(String data) {
        logger.debug("Enter. Save data: " + data);
        LinkedHashMap<String, String> data2 = JSON.decode(data);
        return launcher.saveConfig(data2);
    }

    @POST
    @Path("/start")
    public Response start() {
        logger.debug("Enter");
        return launcher.start();
    }

    @POST
    @Path("/stop")
    public Response stop() {
        logger.debug("Enter");
        return launcher.stop();
    }

    @GET
    @Path("/getStatistics")
    public String getStatistics() {
        return launcher.getStatistics();
    }

}
