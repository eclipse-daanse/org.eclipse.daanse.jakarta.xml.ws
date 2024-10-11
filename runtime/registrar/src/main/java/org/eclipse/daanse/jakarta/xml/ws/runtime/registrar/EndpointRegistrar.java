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

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.SoapWhiteboardConstants;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;

@Component(immediate = true)
@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, //
        name = SoapWhiteboardConstants.SOAP, //
        version = SoapWhiteboardConstants.SOAP_SPECIFICATION_VERSION)
public class EndpointRegistrar {

    private BundleContext bundleContext;
    private Logger logger;

    private Map<Bundle, BundleEndpointContext> contextMap = new WeakHashMap<>();
    private Map<Handler<? extends MessageContext>, HandlerInfo> handlerMap = new ConcurrentHashMap<>();
    private Map<Object, EndpointRegistration> endpointRegistrations = new ConcurrentHashMap<>();

    @Activate
    public EndpointRegistrar(BundleContext bc, @Reference(service = LoggerFactory.class) Logger logger) {
        this.bundleContext = bc;
        this.logger = logger;
    }

    @Reference(service = AnyService.class, target = "(" + SoapWhiteboardConstants.SOAP_ENDPOINT_IMPLEMENTOR
            + "=true)", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void bindEndpointImplementor(Object endpointImplementor, Map<String, ?> properties) {
        Map<String, Object> filteredProperties = properties.entrySet().stream()//
                .filter(e -> !e.getKey().startsWith("."))// secret properties should not be propagated
                .filter(e -> !SoapWhiteboardConstants.SOAP_ENDPOINT_IMPLEMENTOR.equals(e.getKey()))
                // don't propagate the implementor property
                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
        logger.debug(">> BINDING implementor={} properties={}, filtered={}", endpointImplementor, properties,
                filteredProperties);
        EndpointRegistration registration = new EndpointRegistration(endpointImplementor, filteredProperties);
        endpointRegistrations.put(endpointImplementor, registration);
        registration.register();
    }

    public void unbindEndpointImplementor(Object endpointImplementor) {
        logger.debug("UNBINDING implementor={}", endpointImplementor);
        EndpointRegistration registration = endpointRegistrations.remove(endpointImplementor);
        if (registration != null) {
            registration.unregister();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addHandler(Handler<? extends MessageContext> handler, Map<String, ?> properties)
            throws InvalidSyntaxException {
        Object epSelect = properties.get(SoapWhiteboardConstants.SOAP_ENDPOINT_SELECT);
        logger.debug("ADD handler={}, epSelect = {}, properties={}", handler, epSelect, properties);
        if (epSelect instanceof String fs) {
            try {
                Filter filter = FrameworkUtil.createFilter(fs);
                Number priority = (Integer) properties.get(Constants.SERVICE_RANKING);
                handlerMap.put(handler, new HandlerInfo(filter, priority == null ? 0 : priority.intValue()));
                updateAll();
            } catch (RuntimeException e) {
                logger.error("EndpointRegistrar error addHandler", e);
            }
        }
    }

    public void removeHandler(Handler<? extends MessageContext> handler) {
        logger.debug("REMOVE handler={}", handler);
        if (handlerMap.remove(handler) != null) {
            updateAll();
        }
    }

    private void updateAll() {
        logger.debug("Update all handlers...");
        endpointRegistrations.values().stream().forEach(EndpointRegistration::refresh);
    }

    private final class EndpointRegistration implements ServiceFactory<Endpoint> {

        private Object implementor;
        private ServiceRegistration<?> registration;
        private Map<Bundle, Endpoint> endpointMap = new ConcurrentHashMap<>();
        private Map<String, Object> properties;
        private List<Handler> handlerChain;

        public EndpointRegistration(Object implementor, Map<String, Object> properties) {
            this.implementor = implementor;
            this.properties = properties;
        }

        public void refresh() {
            if (registration != null) {
                unregister();
                register();
            }
        }

        public void unregister() {
            try {
                if (registration != null) {
                    registration.unregister();
                }
            } catch (IllegalStateException e) {
                // no one cares then...
            }
            endpointMap.forEach((b, e) -> ungetService(b, null, e));
        }

        public void register() {
            Dictionary<String, Object> dictionaryProperties = getServiceProperties();
            handlerChain = handlers(dictionaryProperties).toList();
            registration = bundleContext.registerService(Endpoint.class, this, dictionaryProperties);
        }

        @Override
        public Endpoint getService(Bundle bundle, ServiceRegistration<Endpoint> registration) {
            Endpoint endpoint = Endpoint.create(implementor);
            endpointMap.put(bundle, endpoint);
            try {
                endpoint.getBinding().setHandlerChain(handlerChain);
            } catch (UnsupportedOperationException | WebServiceException e) {
                // can't set a handler chain then!
                logger.warn("Can't update handler chain for {}: {}", endpoint, e.toString());
            }
            endpoint.setProperties(properties);
            synchronized (contextMap) {
                BundleEndpointContext context = contextMap.computeIfAbsent(bundle, BundleEndpointContext::new);
                context.addEndpoint(endpoint);
                endpoint.setEndpointContext(context);
            }
            return endpoint;
        }

        private Dictionary<String, Object> getServiceProperties() {
            return FrameworkUtil.asDictionary(properties);
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<Endpoint> registration, Endpoint endpoint) {
            endpointMap.remove(bundle);
            synchronized (contextMap) {
                BundleEndpointContext context = contextMap.get(bundle);
                if (context != null) {
                    context.removeEndpoint(endpoint);
                    if (context.getEndpoints().isEmpty()) {
                        contextMap.remove(bundle);
                    }
                }
            }
            endpoint.stop();
        }

        @SuppressWarnings("rawtypes"/* required by API */)
        private Stream<Handler> handlers(Dictionary<String, ?> properties) {
            return handlerMap.entrySet().stream().filter(e -> handlerMatches(properties, e.getKey(), e.getValue()))
                    .sorted(HandlerInfo.SORT_BY_PRIORITY).map(Entry::getKey);
        }

        private boolean handlerMatches(Dictionary<String, ?> properties, Handler<?> handler, HandlerInfo handlerInfo) {
            if (handlerInfo.filter().match(properties)) {
                logger.debug("Handler {} filter {} matches {}", handler, handlerInfo.filter(), properties);
                return true;
            }
            logger.debug("Handler {} filter {} NOT matches {}", handler, handlerInfo.filter(), properties);
            return false;
        }
    }

}
