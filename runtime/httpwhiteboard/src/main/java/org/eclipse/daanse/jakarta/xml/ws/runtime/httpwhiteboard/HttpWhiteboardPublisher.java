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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.SoapWhiteboardConstants;
import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.annotations.RequireSoapWhiteboard;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import jakarta.xml.ws.Endpoint;

/**
 * Publishes Endpoints with the HttpWhiteboard service
 *
 */
@RequireHttpWhiteboard
@RequireSoapWhiteboard
@Component(immediate = true, name = "org.eclipse.daanse.jakarta.xml.ws.runtime.httpwhiteboard")
public class HttpWhiteboardPublisher {

    private BundleContext bundleContext;
    private Logger logger;
    private HttpWhiteboardPublisherConfig config;
    private Map<Endpoint, WhiteboardHttpContext> registrationMap = new ConcurrentHashMap<>();

    @Activate
    public HttpWhiteboardPublisher(BundleContext bundleContext, @Reference(service = LoggerFactory.class) Logger logger,
            HttpWhiteboardPublisherConfig config) {
        this.bundleContext = bundleContext;
        this.logger = logger;
        this.config = config;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void publishEndpoint(Endpoint endpoint, Map<String, ?> properties) {
        Object path = Objects.requireNonNullElse(properties.get(SoapWhiteboardConstants.SOAP_ENDPOINT_PATH), "/");
        logger.info("Registering {} with http whiteboard at context path {}", endpoint, path);
        WhiteboardHttpContext httpContext = new WhiteboardHttpContext(path.toString(), endpoint.getProperties());
        registrationMap.put(endpoint, httpContext);
        logger.debug(">>> HttpWhiteboardPublisher publishing");
        try {
            endpoint.publish(httpContext);
            logger.debug(">>> HttpWhiteboardPublisher register service");
            httpContext.register(bundleContext);
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("HttpWhiteboardPublisher publishEndpoint error", e);
        }
    }

    public void unpublishEndpoint(Endpoint endpoint, Map<String, ?> properties) {
        WhiteboardHttpContext context = registrationMap.remove(endpoint);
        if (context != null) {
            context.close();
        }
        endpoint.stop();
    }

}
