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
package org.eclipse.daanse.jakarta.xml.ws.runtime.embedded;

import java.util.Map;
import java.util.Objects;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.SoapWhiteboardConstants;
import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.annotations.RequireSoapWhiteboard;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import jakarta.xml.ws.Endpoint;

/**
 *
 * publishes Endpoints with the default embedded server
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, name = "org.eclipse.daanse.jakarta.xml.ws.runtime.embedded")
@RequireSoapWhiteboard
@RequireConfigurationAdmin
public class EmbeddedPublisher {

    private EmbeddedPublisherConfig config;
    private Logger logger;

    @Activate
    public EmbeddedPublisher(EmbeddedPublisherConfig config, @Reference(service = LoggerFactory.class) Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void publishEndpoint(Endpoint endpoint, Map<String, ?> properties) {
        Object path = Objects.requireNonNullElse(properties.get(SoapWhiteboardConstants.SOAP_ENDPOINT_PATH), "/");
        String epAddress = config.protocol() + "://" + config.host() + ":" + config.port() + path;
        logger.info("Registering {} with embedded server and address {}", endpoint, epAddress);
        logger.debug(">>> PUBLISH EP @ " + epAddress + " properties=" + properties);
        try {
            endpoint.publish(epAddress);
        } catch (RuntimeException e) {
            logger.error("EmbeddedPublisher publishEndpoint error", e);
        }
    }

    public void unpublishEndpoint(Endpoint endpoint, Map<String, ?> properties) {
        endpoint.stop();
    }

}
