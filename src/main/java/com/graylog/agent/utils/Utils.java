package com.graylog.agent.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public static String getHostname() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = System.getenv("HOSTNAME");
            if (hostname == null) {
                hostname = System.getenv("COMPUTERNAME");
            }
            if (hostname == null) {
                hostname = "unknown host";
                LOG.warn("Unable to detect the local host name, use source override!");
            }
        }

        return hostname;
    }

    public static boolean isWindows() {
        return IS_WINDOWS;
    }
}
