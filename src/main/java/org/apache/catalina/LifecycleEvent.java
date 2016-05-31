package org.apache.catalina;

import java.util.EventObject;

public final class LifecycleEvent extends EventObject {

    private static final long serialVersionUID = 7234146918840763476L;

    private Object data = null; // The event data associated(关联) with this event.

    private Lifecycle lifecycle = null;

    private String type = null;

    public LifecycleEvent(Lifecycle lifecycle, String type) {
        this(lifecycle, type, null);
    }

    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        super(lifecycle);
        this.lifecycle = lifecycle;
        this.type = type;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public String getType() {
        return type;
    }
}