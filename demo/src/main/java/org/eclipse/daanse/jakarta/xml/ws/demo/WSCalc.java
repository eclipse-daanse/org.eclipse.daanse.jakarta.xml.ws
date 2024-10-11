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
package org.eclipse.daanse.jakarta.xml.ws.demo;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.prototypes.SOAPWhiteboardEndpoint;
import org.eclipse.daanse.jakarta.xml.ws.demo.XML.IN;
import org.eclipse.daanse.jakarta.xml.ws.demo.XML.OUT;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@WebService(serviceName = "CalculatorService")
@XmlSeeAlso({ XML.class })
@SOAPWhiteboardEndpoint(contextpath = "/calc")
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, name = "org.eclipse.daanse.jakarta.xml.ws.demo.calculator", service = WSCalc.class, immediate = true)
public class WSCalc {

    private int i;

    @Activate
    public WSCalc(WSCalcConfig config) {
        i = config.number();
    }

    @Activate
    @Modified
    void configure(WSCalcConfig config) {
        i = config.number();
    }

    @WebMethod(operationName = "calc", action = "urn:calc")
    public OUT calc(@WebParam(name = "num") IN in) {
        return new OUT(in.i + i);
    }

}
