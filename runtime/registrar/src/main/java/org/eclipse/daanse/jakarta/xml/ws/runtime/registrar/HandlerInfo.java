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
package org.eclipse.daanse.jakarta.xml.ws.runtime.registrar;

import java.util.Comparator;
import java.util.Map.Entry;

import org.osgi.framework.Filter;

import jakarta.xml.ws.handler.Handler;

final record HandlerInfo(Filter filter, int priority) {

    static final Comparator<Entry<Handler<?>, HandlerInfo>> SORT_BY_PRIORITY = Comparator.comparing(Entry::getValue,
            Comparator.comparingInt(HandlerInfo::priority).reversed());

}
