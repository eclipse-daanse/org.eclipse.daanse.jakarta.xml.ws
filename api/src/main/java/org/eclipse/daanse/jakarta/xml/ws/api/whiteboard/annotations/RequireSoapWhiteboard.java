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
package org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.SoapWhiteboardConstants;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.implementation.ImplementationNamespace;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Requirement(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, //
        name = SoapWhiteboardConstants.SOAP, //
        version = SoapWhiteboardConstants.SOAP_SPECIFICATION_VERSION)
public @interface RequireSoapWhiteboard {
    // This is a marker annotation.
}
