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
package org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.prototypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.SoapWhiteboardConstants;
import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.annotations.RequireSoapWhiteboard;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Annotation that can be used to mark a service component as an object that
 * should be considered by the SOPA Whiteboard Extender
 *
 */
@ComponentPropertyType
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@RequireSoapWhiteboard
public @interface SOAPWhiteboardEndpoint {

    String PREFIX_ = SoapWhiteboardConstants.SOAP_ENDPOINT_PREFIX;

    /**
     * @return <code>true</code> if this is an implementor for the soap whiteboard,
     *         <code>false</code> otherwise, can be used to switch an implementation
     *         on/off
     */
    boolean implementor() default true;

    /**
     * @return the context path under which this endpoint should be registered
     */
    String contextpath() default "";

}
