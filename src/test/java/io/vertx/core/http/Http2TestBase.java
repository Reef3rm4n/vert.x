/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.http;

import io.netty.channel.EventLoopGroup;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.test.tls.Cert;
import io.vertx.test.tls.Trust;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Http2TestBase extends HttpTestBase {

  public static HttpServerOptions createHttp2ServerOptions(int port, String host) {
    return new HttpServerOptions()
        .setPort(port)
        .setHost(host)
        .setSslEngineOptions(new JdkSSLEngineOptions())
        .setUseAlpn(true)
        .setSsl(true)
        .addEnabledCipherSuite("TLS_RSA_WITH_AES_128_CBC_SHA") // Non Diffie-helman -> debuggable in wireshark
        .setKeyStoreOptions(Cert.SERVER_JKS.get());
  };

  public static HttpClientOptions createHttp2ClientOptions() {
    return new HttpClientOptions()
      .setSslEngineOptions(new JdkSSLEngineOptions())
      .setUseAlpn(true)
      .setSsl(true)
      .setTrustStoreOptions(Trust.SERVER_JKS.get())
      .setProtocolVersion(HttpVersion.HTTP_2);
  }

  protected HttpServerOptions serverOptions;
  protected HttpClientOptions clientOptions;
  protected List<EventLoopGroup> eventLoopGroups = new ArrayList<>();

  @Override
  public void setUp() throws Exception {
    eventLoopGroups.clear();
    serverOptions =  createHttp2ServerOptions(DEFAULT_HTTPS_PORT, DEFAULT_HTTPS_HOST);
    clientOptions = createHttp2ClientOptions();
    super.setUp();
  }

  @Override
  protected void configureDomainSockets() throws Exception {
    // Nope
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    for (EventLoopGroup eventLoopGroup : eventLoopGroups) {
      eventLoopGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS);
    }
  }

  @Override
  protected HttpServerOptions createBaseServerOptions() {
    return serverOptions;
  }

  @Override
  protected HttpClientOptions createBaseClientOptions() {
    return clientOptions;
  }
}
