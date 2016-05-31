package org.apache.catalina.core;

import java.util.ArrayList;

import javax.management.ObjectName;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.modeler.Registry;

public class StandardPipeline implements Pipeline, Contained, Lifecycle {

    private static Log log = LogFactory.getLog(StandardPipeline.class);

    public StandardPipeline() {
        this(null);
    }

    public StandardPipeline(Container container) {
        super();
        setContainer(container);
    }

    protected Valve basic = null;  //basic的valve对象,如果有普通valve对象的话，basic对象将会处在链表的尾部

    protected Container container = null; //拥有这个pipeline的container对象

    protected String info = "org.apache.catalina.core.StandardPipeline/1.0";

    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    protected static StringManager sm = StringManager.getManager(Constants.Package);

    protected boolean started = false;

    protected Valve first = null; // The first valve associated with this Pipeline.

    public String getInfo() {
        return (this.info);
    }

    public Container getContainer() {
        return (this.container);
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    public synchronized void start() throws LifecycleException {
        // Validate and update our current component state
        if (started) {
            throw new LifecycleException(sm.getString("standardPipeline.alreadyStarted"));
        }
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        started = true;

        // Start the Valves in our pipeline (including the basic), if any
        Valve current = first;
        if (current == null) {
            current = basic;
        }
        while (current != null) {
            if (current instanceof Lifecycle){
                ((Lifecycle) current).start();
            }
            registerValve(current);
            current = current.getNext();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);

    }

    /**
     * Gracefully shut down active use of the public methods of this Component.
     * 
     * @exception LifecycleException
     *                if this component detects a fatal error
     *                that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException(sm.getString("standardPipeline.notStarted"));

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        Valve current = first;
        if (current == null) {
            current = basic;
        }
        while (current != null) {
            if (current instanceof Lifecycle) {
                ((Lifecycle) current).stop();
            }
            unregisterValve(current);
            current = current.getNext();
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }

    private void registerValve(Valve valve) {

        if (valve instanceof ValveBase && ((ValveBase) valve).getObjectName() == null) {
            try {

                String domain = ((ContainerBase) container).getDomain();
                if (container instanceof StandardContext) {
                    domain = ((StandardContext) container).getEngineName();
                }
                if (container instanceof StandardWrapper) {
                    Container ctx = ((StandardWrapper) container).getParent();
                    domain = ((StandardContext) ctx).getEngineName();
                }
                ObjectName vname = ((ValveBase) valve).createObjectName(domain, ((ContainerBase) container).getJmxName());
                if (vname != null) {
                    ((ValveBase) valve).setObjectName(vname);
                    Registry.getRegistry(null, null).registerComponent(valve, vname, valve.getClass().getName());
                    ((ValveBase) valve).setController(((ContainerBase) container).getJmxName());
                }
            } catch (Throwable t) {
                log.info("Can't register valve " + valve, t);
            }
        }
    }

    private void unregisterValve(Valve valve) {
        if (valve instanceof ValveBase) {
            try {
                ValveBase vb = (ValveBase) valve;
                if (vb.getController() != null && vb.getController() == ((ContainerBase) container).getJmxName()) {

                    ObjectName vname = vb.getObjectName();
                    Registry.getRegistry(null, null).getMBeanServer().unregisterMBean(vname);
                    ((ValveBase) valve).setObjectName(null);
                }
            } catch (Throwable t) {
                log.info("Can't unregister valve " + valve, t);
            }
        }
    }

    // ------------------------------------------------------- Pipeline Methods

    /**
     * <p>
     * Return the Valve instance that has been distinguished as the basic Valve for this Pipeline (if any).
     */
    public Valve getBasic() {

        return (this.basic);

    }

    /**
     * <p>
     * Set the Valve instance that has been distinguished as the basic Valve for this Pipeline (if any). Prioer to setting the basic Valve, the Valve's
     * <code>setContainer()</code> will be called, if it implements <code>Contained</code>, with the owning Container as an argument. The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to be associated with this Container, or <code>IllegalStateException</code> if it is
     * already associated with a different Container.
     * </p>
     * 
     * @param valve
     *            Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve) {

        // Change components if necessary
        Valve oldBasic = this.basic;
        if (oldBasic == valve)
            return;

        // Stop the old component if necessary
        if (oldBasic != null) {
            if (started && (oldBasic instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldBasic).stop();
                } catch (LifecycleException e) {
                    log.error("StandardPipeline.setBasic: stop", e);
                }
            }
            if (oldBasic instanceof Contained) {
                try {
                    ((Contained) oldBasic).setContainer(null);
                } catch (Throwable t) {
                    ;
                }
            }
        }

        // Start the new component if necessary
        if (valve == null)
            return;
        if (valve instanceof Contained) {
            ((Contained) valve).setContainer(this.container);
        }
        if (valve instanceof Lifecycle) {
            try {
                ((Lifecycle) valve).start();
            } catch (LifecycleException e) {
                log.error("StandardPipeline.setBasic: start", e);
                return;
            }
        }

        // Update the pipeline
        Valve current = first;
        while (current != null) {
            if (current.getNext() == oldBasic) {
                current.setNext(valve);
                break;
            }
            current = current.getNext();
        }

        this.basic = valve;

    }

    /**
     * <p>
     * Add a new Valve to the end of the pipeline associated with this Container. Prior to adding the Valve, the Valve's <code>setContainer()</code> method will
     * be called, if it implements <code>Contained</code>, with the owning Container as an argument. The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to be associated with this Container, or <code>IllegalStateException</code> if it is
     * already associated with a different Container.
     * </p>
     * 
     * @param valve
     *            Valve to be added
     * 
     * @exception IllegalArgumentException
     *                if this Container refused to
     *                accept the specified Valve
     * @exception IllegalArgumentException
     *                if the specifie Valve refuses to be
     *                associated with this Container
     * @exception IllegalStateException
     *                if the specified Valve is already
     *                associated with a different Container
     */
    public void addValve(Valve valve) {
        // Validate that we can add this Valve
        if (valve instanceof Contained)
            ((Contained) valve).setContainer(this.container);

        // Start the new component if necessary
        if (started) {
            if (valve instanceof Lifecycle) {
                try {
                    ((Lifecycle) valve).start();
                } catch (LifecycleException e) {
                    log.error("StandardPipeline.addValve: start: ", e);
                }
            }
            // Register the newly added valve
            registerValve(valve);
        }

        // Add this Valve to the set associated with this Pipeline
        if (first == null) {
            first = valve;
            valve.setNext(basic);
        } else {
            Valve current = first;
            while (current != null) {
                if (current.getNext() == basic) {
                    current.setNext(valve);
                    valve.setNext(basic);
                    break;
                }
                current = current.getNext();
            }
        }

    }

    /**
     * Return the set of Valves in the pipeline associated with this
     * Container, including the basic Valve (if any). If there are no
     * such Valves, a zero-length array is returned.
     */
    public Valve[] getValves() {

        ArrayList<Valve> valveList = new ArrayList<Valve>();
        Valve current = first;
        if (current == null) {
            current = basic;
        }
        while (current != null) {
            valveList.add(current);
            current = current.getNext();
        }

        return ((Valve[]) valveList.toArray(new Valve[0]));

    }

    public ObjectName[] getValveObjectNames() {

        ArrayList<ObjectName> valveList = new ArrayList<ObjectName>();
        Valve current = first;
        if (current == null) {
            current = basic;
        }
        while (current != null) {
            if (current instanceof ValveBase) {
                valveList.add(((ValveBase) current).getObjectName());
            }
            current = current.getNext();
        }

        return ((ObjectName[]) valveList.toArray(new ObjectName[0]));

    }

    /**
     * Remove the specified Valve from the pipeline associated with this
     * Container, if it is found; otherwise, do nothing. If the Valve is
     * found and removed, the Valve's <code>setContainer(null)</code> method
     * will be called if it implements <code>Contained</code>.
     * 
     * @param valve
     *            Valve to be removed
     */
    public void removeValve(Valve valve) {

        Valve current;
        if (first == valve) {
            first = first.getNext();
            current = null;
        } else {
            current = first;
        }
        while (current != null) {
            if (current.getNext() == valve) {
                current.setNext(valve.getNext());
                break;
            }
            current = current.getNext();
        }

        if (first == basic)
            first = null;

        if (valve instanceof Contained)
            ((Contained) valve).setContainer(null);

        // Stop this valve if necessary
        if (started) {
            if (valve instanceof Lifecycle) {
                try {
                    ((Lifecycle) valve).stop();
                } catch (LifecycleException e) {
                    log.error("StandardPipeline.removeValve: stop: ", e);
                }
            }
            // Unregister the removed valave
            unregisterValve(valve);
        }
    }

    public Valve getFirst() {
        if (first != null) {
            return first;
        } else {
            return basic;
        }
    }
}
