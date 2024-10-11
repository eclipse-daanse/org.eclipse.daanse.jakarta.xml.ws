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

import java.io.ByteArrayOutputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

/*
 * This simple SOAPHandler will output the contents of incoming
 * and outgoing messages.
 */
@Component(service = Handler.class)
@Designate(ocd = SOAPLoggingHandler.Config.class, factory = true)
public class SOAPLoggingHandler implements SOAPHandler<SOAPMessageContext> {

    @ObjectClassDefinition
    @interface Config {

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

    private Logger logger = LoggerFactory.getLogger(SOAPLoggingHandler.class);

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        logOut(smc);
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext smc) {
        logOut(smc);
        return true;
    }

    // nothing to clean up
    @Override
    public void close(MessageContext messageContext) {
    }

    /*
     * Check the MESSAGE_OUTBOUND_PROPERTY in the context to see if this is an
     * outgoing or incoming message. Write a brief message to the print stream and
     * output the message. The writeTo() method can throw SOAPException or
     * IOException
     */
    private void logOut(SOAPMessageContext smc) {

        Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty.booleanValue()) {
            logger.error("\nOutbound message:");
        } else {
            logger.error("Inbound message:");
        }

        SOAPMessage message = smc.getMessage();
        try {
            Map<String, List<String>> headers = (Map<String, List<String>>) smc
                    .get(MessageContext.HTTP_REQUEST_HEADERS);
            System.out.println(headers);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            System.out.println(new String(baos.toByteArray()));
            logger.error(new String(baos.toByteArray()));
        } catch (Exception e) {
            logger.error("Exception in handler: " + e);
        }
    }
}
