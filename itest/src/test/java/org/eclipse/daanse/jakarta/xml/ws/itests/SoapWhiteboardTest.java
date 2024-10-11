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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.jakarta.xml.ws.api.whiteboard.SoapWhiteboardConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.Property.Scalar;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;
import org.osgi.test.junit5.cm.ConfigurationExtension;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.Handler;

@ExtendWith(ConfigurationExtension.class)
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@RequireHttpWhiteboard
class SoapWhiteboardTest {

    private static final String ECHO_NS = "http://demo.ws.xml.jakarta.daanse.eclipse.org/";
    private static final String SERVER_PORT_EMBEDDED_SERVER_PORT = "9999";
    private static final String SERVER_PORT_WHITEBOARD = "8090";

    private static final String PID_EMBEDDED = "org.eclipse.daanse.jakarta.xml.ws.runtime.embedded";
    @InjectService
    ServiceComponentRuntime componentRuntime;

    @Test
    @WithFactoryConfiguration(factoryPid = PID_EMBEDDED, name = "embedded-publisher", location = "?", properties = {
            @Property(key = "port", value = SERVER_PORT_EMBEDDED_SERVER_PORT, scalar = Scalar.Integer) })
    void testEchoService(@InjectBundleContext BundleContext bundleContext) throws Exception {
        printScrInfo();
        TestLogicalHandler locicalHandler = new TestLogicalHandler();
        bundleContext.registerService(Handler.class, locicalHandler,
                FrameworkUtil.asDictionary(Map.of(SoapWhiteboardConstants.SOAP_ENDPOINT_SELECT, "(wstype=echo)")));
        TestSoapHandler soapHandler = new TestSoapHandler();
        bundleContext.registerService(Handler.class, soapHandler,
                FrameworkUtil.asDictionary(Map.of(SoapWhiteboardConstants.SOAP_ENDPOINT_SELECT, "(wstype=echo)")));
        // we want one EndpointImplementor (=echo service)
        waitForServicesBound(bundleContext, "org.eclipse.daanse.jakarta.xml.ws.runtime.registrar.EndpointRegistrar",
                "EndpointImplementor", 1, 10, TimeUnit.SECONDS);
        // and it should be published (=embedded)
        waitForServicesBound(bundleContext, "org.eclipse.daanse.jakarta.xml.ws.runtime.embedded", "publishEndpoint", 1,
                10, TimeUnit.SECONDS);
        // and it should be published (=whiteboard)
        waitForServicesBound(bundleContext, "org.eclipse.daanse.jakarta.xml.ws.runtime.httpwhiteboard",
                "publishEndpoint", 1, 10, TimeUnit.SECONDS);
        printScrInfo();
        TimeUnit.SECONDS.sleep(1); // let it all settle a bit...
        performEchoTest(String.format("http://localhost:%s/echo?wsdl", SERVER_PORT_EMBEDDED_SERVER_PORT));
        performEchoTest(String.format("http://localhost:%s/echo?wsdl", SERVER_PORT_WHITEBOARD));
        assertThat(locicalHandler.handledMessages.get()).isEqualTo(4);
        assertThat(soapHandler.handledMessages.get()).isEqualTo(4);
    }

    @Test
    @WithFactoryConfiguration(factoryPid = PID_EMBEDDED, name = "embedded-publisher", location = "?", properties = {
            @Property(key = "port", value = SERVER_PORT_EMBEDDED_SERVER_PORT, scalar = Scalar.Integer) })
    void testProvider(@InjectBundleContext BundleContext bundleContext) throws Exception {
        ProviderWebService webService = new ProviderWebService();
        bundleContext.registerService(ProviderWebService.class, webService,
                FrameworkUtil.asDictionary(Map.of(SoapWhiteboardConstants.SOAP_ENDPOINT_IMPLEMENTOR, true,
                        SoapWhiteboardConstants.SOAP_ENDPOINT_PATH, "/provider")));
        System.out.println("Endpoint registered!");
        TimeUnit.SECONDS.sleep(1); // let it all settle a bit...
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://localhost:%s/provider", SERVER_PORT_EMBEDDED_SERVER_PORT)))
                .POST(BodyPublishers
                        .ofString("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                + "   <soapenv:Header/>" + "   <soapenv:Body>" + "   </soapenv:Body>"
                                + "</soapenv:Envelope>", StandardCharsets.UTF_8))
                .build();
        System.out.println("Sending request...");
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        System.out.println("Service response: " + response.body());
        assertThat(response.body()).contains("Hello Test");
    }

    private void performEchoTest(String echoEndpointUrl)
            throws IOException, MalformedURLException, SOAPException, URISyntaxException {
        try (InputStream stream = new URI(echoEndpointUrl).toURL().openStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            stream.transferTo(out);
            String wsdl = new String(out.toByteArray(), StandardCharsets.UTF_8).replaceAll("\\s", " ")
                    .replaceAll("\\s+", " ");
            assertThat(wsdl).matches(".*<definitions.*name=\"WSEchoService\".*>.*")
                    .contains("<service name=\"WSEchoService\">");
        }
        System.out.println("Server is running!");
        SOAPMessage message = SOAPClientSAAJ.newSoapMessage(ECHO_NS, body -> {
            SOAPElement echoMessage = body.addChildElement("echo", "tns", ECHO_NS);
            echoMessage.addChildElement("textIn").setTextContent("Hello Test");
        });
        message.writeTo(System.out);
        SOAPMessage response = SOAPClientSAAJ.sendMessage(echoEndpointUrl, message);
        response.writeTo(System.out);
        SOAPBody responseBody = response.getSOAPBody();
        NodeList echoResponseList = responseBody.getElementsByTagNameNS(ECHO_NS, "echoResponse");
        assertThat(echoResponseList.getLength()).isEqualTo(1);
        Element echoResponse = (SOAPElement) echoResponseList.item(0);
        NodeList returnList = echoResponse.getElementsByTagName("return");
        assertThat(returnList.getLength()).isEqualTo(1);
        assertThat(returnList.item(0)).extracting(Node::getTextContent).isEqualTo("Hello Test");
    }

    private void waitForServicesBound(BundleContext context, String componentName, String reference, int minimumCount,
            long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            Bundle[] bundles = context.getBundles();
            for (Bundle bundle : bundles) {
                ComponentDescriptionDTO component = componentRuntime.getComponentDescriptionDTO(bundle, componentName);
                if (component == null) {
                    continue;
                }
                for (ComponentConfigurationDTO cfg : componentRuntime.getComponentConfigurationDTOs(component)) {
                    SatisfiedReferenceDTO[] satref = cfg.satisfiedReferences;
                    for (SatisfiedReferenceDTO ref : satref) {
                        if (ref.name.equals(reference) && ref.boundServices.length >= minimumCount) {
                            return;
                        }
                    }
                }
            }
            Thread.onSpinWait();
        }
        fail("The reference " + reference + " of " + componentName + " did not get satisfied with at laest "
                + minimumCount + " references within " + timeout + " " + unit);
    }

    void printScrInfo() {

        System.out.println("============ Framework Components ==================");
        Collection<ComponentDescriptionDTO> descriptionDTOs = componentRuntime.getComponentDescriptionDTOs();
        Comparator<ComponentConfigurationDTO> byComponentName = Comparator.comparing(dto -> dto.description.name,
                String.CASE_INSENSITIVE_ORDER);
        Comparator<ComponentConfigurationDTO> byComponentState = Comparator.comparingInt(dto -> dto.state);
        descriptionDTOs.stream().flatMap(dto -> componentRuntime.getComponentConfigurationDTOs(dto).stream())
                .sorted(byComponentState.thenComparing(byComponentName)).forEachOrdered(dto -> {
                    if (dto.state == ComponentConfigurationDTO.FAILED_ACTIVATION) {
                        System.out.println(
                                toComponentState(dto.state) + " | " + dto.description.name + " | " + dto.failure);
                    } else {
                        System.out.println(toComponentState(dto.state) + " | " + dto.description.name);
                    }
                    for (int i = 0; i < dto.unsatisfiedReferences.length; i++) {
                        UnsatisfiedReferenceDTO ref = dto.unsatisfiedReferences[i];
                        System.out.println("\t" + ref.name + " is missing");
                    }
                    for (int i = 0; i < dto.satisfiedReferences.length; i++) {
                        SatisfiedReferenceDTO sat = dto.satisfiedReferences[i];
                        System.out.println("\t" + sat.name + " (bound " + sat.boundServices.length + ")");
                    }
                    System.out.println("\tProperties:");
                    for (Entry<String, Object> entry : dto.properties.entrySet()) {
                        System.out.println("\t\t" + entry.getKey() + ": " + entry.getValue());
                    }
                });
    }

    private static String toComponentState(int state) {
        return switch (state) {
        case ComponentConfigurationDTO.ACTIVE -> "ACTIVE     ";
        case ComponentConfigurationDTO.FAILED_ACTIVATION -> "FAILED     ";
        case ComponentConfigurationDTO.SATISFIED -> "SATISFIED  ";
        case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION, ComponentConfigurationDTO.UNSATISFIED_REFERENCE ->
            "UNSATISFIED";
        default -> String.valueOf(state);
        };
    }

}
