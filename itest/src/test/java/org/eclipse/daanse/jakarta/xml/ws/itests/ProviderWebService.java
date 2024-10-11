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

import java.io.IOException;

import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.SOAPFaultException;

@WebServiceProvider
@ServiceMode(value = Service.Mode.MESSAGE)
public class ProviderWebService implements Provider<SOAPMessage> {

    private static final String MY_TNS = "my:tns";

    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        System.out.println("===== The provider got a request =====");
        try {
            request.writeTo(System.out);
            System.out.println();
            return SOAPClientSAAJ.newSoapMessage(MY_TNS, body -> {
                SOAPElement echoMessage = body.addChildElement("provider", "tns", MY_TNS);
                echoMessage.addChildElement("say").setTextContent("Hello Test");
            });
        } catch (SOAPException | IOException e) {
            throw new SOAPFaultException(getFault(e));
        }
    }

    private SOAPFault getFault(Exception ex) {
        try {
            SOAPFault fault = SOAPFactory.newInstance().createFault();
            fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
            fault.setFaultString(ex.toString());
            return fault;
        } catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }

}
