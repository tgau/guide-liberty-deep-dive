// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.deepdive.rest;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// imports for a JAXRS client to simplify the code
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
// logger imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// testcontainers imports
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import jakarta.ws.rs.client.ClientBuilder;
// simple import to build a URI/URL
import jakarta.ws.rs.core.UriBuilder;

public class LibertyContainer extends GenericContainer<LibertyContainer> {

    static final Logger LOGGER = LoggerFactory.getLogger(LibertyContainer.class);

    private String baseURL;

    private KeyStore keystore;
    private SSLContext sslContext;

    public LibertyContainer(final String dockerImageName) {
        super(dockerImageName);
        // wait for smarter planet message by default
        waitingFor(Wait.forLogMessage("^.*CWWKF0011I.*$", 1));
        init();
    }

    // tag::createRestClient[]
    public <T> T createRestClient(Class<T> clazz, String applicationPath) {
        String urlPath = getBaseURL();
        if (applicationPath != null) {
            urlPath += applicationPath;
        }
        ClientBuilder builder = ResteasyClientBuilder.newBuilder();
        ResteasyClient client = (ResteasyClient) builder
                .sslContext(sslContext)
                .trustStore(keystore)
                .build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(urlPath));
        return target.proxy(clazz);
    }
    // end::createRestClient[]

    // tag::getBaseURL[]
    public String getBaseURL() throws IllegalStateException {
        if (baseURL != null) {
            return baseURL;
        }
        if (!this.isRunning()) {
            throw new IllegalStateException(
                "Container must be running to determine hostname and port");
        }
        baseURL = "https://" + this.getContainerIpAddress()
            + ":" + this.getFirstMappedPort();
        return baseURL;
    }
    // end::getBaseURL[]

    private void init() {
        try {
            String keystoreFile = System.getProperty("user.dir")
                    + "/../../finish/system/src/main"
                    + "/liberty/config/resources/security/key.p12";
            keystore = KeyStore.getInstance("PKCS12");
            keystore.load(new FileInputStream(keystoreFile), "secret".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                                        KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, "secret".toCharArray());
            X509TrustManager xtm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException { }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException { }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            TrustManager[] tm = new TrustManager[] {
                                    xtm
                                };
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tm, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
