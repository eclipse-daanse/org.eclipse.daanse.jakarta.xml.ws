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
package org.eclipse.daanse.jakarta.xml.ws.itests;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.xml.ws.handler.LogicalHandler;
import jakarta.xml.ws.handler.LogicalMessageContext;
import jakarta.xml.ws.handler.MessageContext;

public class TestLogicalHandler implements LogicalHandler<LogicalMessageContext> {

    AtomicInteger handledMessages = new AtomicInteger();

    @Override
    public boolean handleMessage(LogicalMessageContext context) {
        int msg = handledMessages.incrementAndGet();
        System.out.println("TestLogicalHandler.handleMessage no. " + msg);
        return true;
    }

    @Override
    public boolean handleFault(LogicalMessageContext context) {
        System.out.println("TestLogicalHandler.handleFault()");
        return true;
    }

    @Override
    public void close(MessageContext context) {
        System.out.println("TestLogicalHandler.close()");
    }

}
