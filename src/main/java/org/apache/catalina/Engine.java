package org.apache.catalina;

public interface Engine extends Container {

    /**
     * Return the default hostname for this Engine.
     */
    public String getDefaultHost();

    public void setDefaultHost(String defaultHost);

    /**
     * Retrieve the JvmRouteId for this engine.
     */
    public String getJvmRoute();


    /**
     * Set the JvmRouteId for this engine.
     *
     * @param jvmRouteId the (new) JVM Route ID. Each Engine within a cluster
     *        must have a unique JVM Route ID.
     */
    public void setJvmRoute(String jvmRouteId);

    public Service getService();

    public void setService(Service service);
}