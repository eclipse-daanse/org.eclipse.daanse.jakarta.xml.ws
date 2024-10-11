/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*   Christoph Läubrich - initial
*/
package org.eclipse.daanse.jakarta.xml.ws.runtime.httpwhiteboard;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.jakarta.xml.ws.runtime.httpwhiteboard.wsri.EndpointHttpExchange;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.spi.http.HttpContext;

public class WhiteboardHttpContext extends HttpContext implements Closeable {

    private String path;
    private Map<String, ?> attributes;
    private Set<String> names;
    private boolean closed;
    private ServiceRegistration<?> serviceRegistration;

    public WhiteboardHttpContext(String path, Map<String, ?> attributes) {
        this.path = path;
        this.attributes = attributes;
        names = Collections.unmodifiableSet(attributes.keySet());
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return names;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    public synchronized void register(BundleContext bundleContext) {
        if (closed) {
            return;
        }
        HashMap<String, Object> properties = new HashMap<>(attributes);
        if (!properties.containsKey(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME)) {
            properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME,
                    "JAX-WS Service for path " + getPath());
        }
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, getPath());
        serviceRegistration = bundleContext.registerService(Servlet.class, new JaxWsServlet(),
                FrameworkUtil.asDictionary(properties));
    }

    private class JaxWsServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (handler == null || closed) {
                resp.sendError(HttpURLConnection.HTTP_UNAVAILABLE);
                return;
            }
            handler.handle(new EndpointHttpExchange(req, resp, getServletContext(), WhiteboardHttpContext.this));
        }

    }

}
