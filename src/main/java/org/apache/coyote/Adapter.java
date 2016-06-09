package org.apache.coyote;

import org.apache.tomcat.util.net.SocketStatus;

public interface Adapter {

    public void service(Request req, Response res) throws Exception;

    public boolean event(Request req, Response res, SocketStatus status) throws Exception;

}
