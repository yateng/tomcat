package org.apache.coyote;

import java.util.Iterator;

public interface ProtocolHandler {

    public void setAttribute(String name, Object value);

    public Object getAttribute(String name);

    @SuppressWarnings("rawtypes")
    public Iterator getAttributeNames();

    public void setAdapter(Adapter adapter);

    public Adapter getAdapter();

    public void init() throws Exception;

    public void start() throws Exception;

    public void pause() throws Exception;

    public void resume() throws Exception;

    public void destroy() throws Exception;

}
