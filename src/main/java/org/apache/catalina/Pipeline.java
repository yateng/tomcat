package org.apache.catalina;

/**
 * pipeline维护了一个valve对象组成的链表，同时它还有一个特殊的valve对象，那就是basic
 *
 */
public interface Pipeline {

    public Valve getBasic();

    public void setBasic(Valve valve);

    public void addValve(Valve valve);

    public Valve[] getValves();

    public void removeValve(Valve valve);

    public Valve getFirst(); // 源码的注释和getBasic一样

}
