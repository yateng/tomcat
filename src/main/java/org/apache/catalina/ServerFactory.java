package org.apache.catalina;

import org.apache.catalina.core.StandardServer;

public class ServerFactory {

    private static Server server = null;

    public static Server getServer() {
        if (server == null)
            server = new StandardServer();
        return server;
    }

    public static void setServer(Server theServer) {
        if (server == null) {
            server = theServer;
        }
    }
}
