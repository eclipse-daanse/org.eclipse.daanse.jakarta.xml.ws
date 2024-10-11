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

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRegistry;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRegistry()
public class XML {

    @XmlRootElement
    public static class IN {
        public IN() {
        }

        IN(int i) {
            this.i = i;
        }

        @XmlAttribute
        public int i;

    }

    @XmlRootElement
    public static class OUT {
        public OUT() {
        }

        OUT(int i) {
            this.i = i;
        }

        @XmlAttribute
        public int i;

    }
}
