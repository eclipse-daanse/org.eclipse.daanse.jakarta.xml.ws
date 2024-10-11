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
*/
package org.eclipse.daanse.jakarta.xml.ws.handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

@Designate(ocd = HeaderEntryAppenderHandler.Config.class, factory = true)
@Component(service = Handler.class)
public class HeaderEntryAppenderHandler implements SOAPHandler<SOAPMessageContext> {

    @ObjectClassDefinition
    @interface Config {
        String key();

        String value();
    }

    private Config config;

    @Activate
    public void activate(Config config) {
        modified(config);
    }

    @Modified
    public void modified(Config config) {
        this.config = config;
    }

    @Deactivate
    public void deactivate() {

    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outbound) {
            // Get headers, create if null
            @SuppressWarnings("unchecked")
            Map<String, List<String>> headers = (Map<String, List<String>>) context
                    .get(MessageContext.HTTP_REQUEST_HEADERS);
            if (headers == null) {
                headers = new HashMap<>();
            }

            // Add authentication header
            headers.put(config.key(), Arrays.asList(config.value()));
            context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }
}
