/*
 * Copyright 1999-2001,2004 The Apache Software Foundation.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina;

import java.io.IOException;
import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * value对象是与container相关联的处理请求的组件，一系列的value组成了pipeline
 */
public interface Valve {

    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo();

    public Valve getNext(); //返回pipeline的下一个value对象，在pipeline上面，valve对象组成了一个链表

    public void setNext(Valve valve);

    public void backgroundProcess();

    /**
     * <p>
     * Perform request processing as required by this Valve.
     * </p>
     * 
     * <p>
     * An individual Valve <b>MAY</b> perform the following actions, in the specified order:
     * </p>
     * <ul>
     * <li>Examine and/or modify the properties of the specified Request and Response.
     * <li>Examine the properties of the specified Request, completely generate the corresponding Response, and return control to the caller.
     * <li>Examine the properties of the specified Request and Response, wrap either or both of these objects to supplement their functionality, and pass them
     * on.
     * <li>If the corresponding Response was not generated (and control was not returned, call the next Valve in the pipeline (if there is one) by executing
     * <code>context.invokeNext()</code>.
     * <li>Examine, but not modify, the properties of the resulting Response (which was created by a subsequently invoked Valve or Container).
     * </ul>
     * 
     * <p>
     * A Valve <b>MUST NOT</b> do any of the following things:
     * </p>
     * <ul>
     * <li>Change request properties that have already been used to direct the flow of processing control for this request (for instance, trying to change the
     * virtual host to which a Request should be sent from a pipeline attached to a Host or Context in the standard implementation).
     * <li>Create a completed Response <strong>AND</strong> pass this Request and Response on to the next Valve in the pipeline.
     * <li>Consume bytes from the input stream associated with the Request, unless it is completely generating the response, or wrapping the request before
     * passing it on.
     * <li>Modify the HTTP headers included with the Response after the <code>invokeNext()</code> method has returned.
     * <li>Perform any actions on the output stream associated with the specified Response after the <code>invokeNext()</code> method has returned.
     * </ul>
     * 
     * @param request
     *            The servlet request to be processed
     * @param response
     *            The servlet response to be created
     * 
     * @exception IOException
     *                if an input/output error occurs, or is thrown
     *                by a subsequently invoked Valve, Filter, or Servlet
     * @exception ServletException
     *                if a servlet error occurs, or is thrown
     *                by a subsequently invoked Valve, Filter, or Servlet
     */
    public void invoke(Request request, Response response) throws IOException, ServletException;

    /**
     * 处理comet事件
     */
    public void event(Request request, Response response, CometEvent event) throws IOException, ServletException;

}
