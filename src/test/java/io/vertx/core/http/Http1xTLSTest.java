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

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.VertxOptions;
import io.vertx.test.tls.Cert;
import io.vertx.test.tls.Trust;
import org.junit.Assume;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Http1xTLSTest extends HttpTLSTest {

  @Override
  protected HttpServerOptions createBaseServerOptions() {
    return new HttpServerOptions()
      .setPort(HttpTestBase.DEFAULT_HTTPS_PORT)
      .setSsl(true);
  };

  @Override
  protected HttpClientOptions createBaseClientOptions() {
    return new HttpClientOptions()
      .setSsl(true)
      .setProtocolVersion(HttpVersion.HTTP_1_1);
  }

// ALPN tests

  @Test
  // Client and server uses ALPN
  public void testAlpn() throws Exception {
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).serverUsesAlpn().clientUsesAlpn().pass();
  }

  // RequestOptions tests

  @Test
  // Client trusts all server certs
  public void testClearClientRequestOptionsSetSSL() throws Exception {
    RequestOptions options = new RequestOptions().setHost(DEFAULT_HTTP_HOST).setPort(4043).setURI(DEFAULT_TEST_URI).setSsl(true);
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(false).requestOptions(options).pass();
  }

  @Test
  // Client trusts all server certs
  public void testSSLClientRequestOptionsSetSSL() throws Exception {
    RequestOptions options = new RequestOptions().setHost(DEFAULT_HTTP_HOST).setPort(4043).setURI(DEFAULT_TEST_URI).setSsl(true);
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(true).requestOptions(options).pass();
  }

  @Test
  // Client trusts all server certs
  public void testClearClientRequestOptionsSetClear() throws Exception {
    RequestOptions options = new RequestOptions().setHost(DEFAULT_HTTP_HOST).setURI(DEFAULT_TEST_URI).setPort(4043).setSsl(false);
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(false).serverSSL(false).requestOptions(options).pass();
  }

  @Test
  // Client trusts all server certs
  public void testSSLClientRequestOptionsSetClear() throws Exception {
    RequestOptions options = new RequestOptions().setHost(DEFAULT_HTTP_HOST).setURI(DEFAULT_TEST_URI).setPort(4043).setSsl(false);
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(true).serverSSL(false).requestOptions(options).pass();
  }

  // requestAbs test

  @Test
  // Client trusts all server certs
  public void testClearClientRequestAbsSetSSL() throws Exception {
    String absoluteURI = "https://" + DEFAULT_HTTP_HOST + ":4043/" + DEFAULT_TEST_URI;
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(false).requestProvider(c -> c.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(absoluteURI))).pass();
  }

  @Test
  // Client trusts all server certs
  public void testSSLClientRequestAbsSetSSL() throws Exception {
    String absoluteURI = "https://" + DEFAULT_HTTP_HOST + ":4043/" + DEFAULT_TEST_URI;
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(true).requestProvider(c -> c.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(absoluteURI))).pass();
  }

  @Test
  // Client trusts all server certs
  public void testClearClientRequestAbsSetClear() throws Exception {
    String absoluteURI = "http://" + DEFAULT_HTTP_HOST + ":4043/" + DEFAULT_TEST_URI;
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(false).serverSSL(false).requestProvider(c -> c.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(absoluteURI))).pass();
  }

  @Test
  // Client trusts all server certs
  public void testSSLClientRequestAbsSetClear() throws Exception {
    String absoluteURI = "http://" + DEFAULT_HTTP_HOST + ":4043/" + DEFAULT_TEST_URI;
    testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE).clientSSL(true).serverSSL(false).requestProvider(c -> c.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI(absoluteURI))).pass();
  }

  // Redirect tests

  @Test
  public void testRedirectToSSL() throws Exception {
    HttpServer redirectServer = vertx.createHttpServer(new HttpServerOptions()
        .setHost(DEFAULT_HTTP_HOST)
        .setPort(DEFAULT_HTTP_PORT)
    ).requestHandler(req -> {
      req.response().setStatusCode(303).putHeader("location", "https://" + DEFAULT_HTTP_HOST + ":4043/" + DEFAULT_TEST_URI).end();
    });
    startServer(redirectServer);
    try {
      RequestOptions options = new RequestOptions().setHost(DEFAULT_HTTP_HOST).setURI(DEFAULT_TEST_URI).setPort(DEFAULT_HTTP_PORT);
      testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.SERVER_JKS, Trust.NONE)
          .clientSSL(false)
          .serverSSL(true)
          .requestOptions(options)
          .followRedirects(true)
          .pass();
    } finally {
      redirectServer.close();
    }
  }

  @Test
  public void testRedirectFromSSL() throws Exception {
    HttpServer redirectServer = vertx.createHttpServer(new HttpServerOptions()
        .setSsl(true)
        .setKeyStoreOptions(Cert.SERVER_JKS.get())
        .setHost(DEFAULT_HTTP_HOST)
        .setPort(DEFAULT_HTTP_PORT)
    ).requestHandler(req -> {
      req.response().setStatusCode(303).putHeader("location", "http://" + DEFAULT_HTTP_HOST + ":4043/" + DEFAULT_TEST_URI).end();
    });
    startServer(redirectServer);
    try {
      RequestOptions options = new RequestOptions().setHost(DEFAULT_HTTP_HOST).setURI(DEFAULT_TEST_URI).setPort(4043).setSsl(false);
      testTLS(Cert.NONE, Trust.SERVER_JKS, Cert.NONE, Trust.NONE)
          .clientSSL(true)
          .serverSSL(false)
          .requestOptions(options)
          .followRedirects(true)
          .pass();
    } finally {
      redirectServer.close();
    }
  }

  @Test
  public void testAppendToHttpChunks() throws Exception {
    List<String> expected = Arrays.asList("chunk-1", "chunk-2", "chunk-3");
    server = vertx.createHttpServer(new HttpServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(Cert.SERVER_JKS.get())
      .setHost(DEFAULT_HTTPS_HOST)
      .setPort(DEFAULT_HTTPS_PORT)
    ).requestHandler(req -> {
      HttpServerResponse resp = req.response().setChunked(true);
      expected.forEach(resp::write);
      resp.end();
    });
    startServer(server);
    client = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
    client.request(HttpMethod.GET, DEFAULT_HTTPS_PORT, DEFAULT_HTTPS_HOST, DEFAULT_TEST_URI).onComplete(onSuccess(req -> {
      req.send().onComplete(onSuccess(resp -> {
        List<String> chunks = new ArrayList<>();
        resp.handler(chunk -> {
          chunk.appendString("-suffix");
          chunks.add(chunk.toString());
        });
        resp.endHandler(v -> {
          assertEquals(expected.stream().map(s -> s + "-suffix").collect(Collectors.toList()), chunks);
          testComplete();
        });
      }));
    }));
    await();
  }

  @Test
  public void testSharedServer() throws Exception {
    int num = VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE;
    Assume.assumeTrue(num > 1);
    List<String> expected = Arrays.asList("chunk-1", "chunk-2", "chunk-3");
    HttpClientOptions options = new HttpClientOptions()
      .setEnabledSecureTransportProtocols(Collections.singleton("TLSv1.2"))
      .setSsl(true)
      .setTrustAll(true);
    AtomicInteger connCount = new AtomicInteger();
    List<String> sessionIds = Collections.synchronizedList(new ArrayList<>());
    client.close();
    client = vertx.httpClientBuilder()
      .with(options)
      .with(new PoolOptions().setHttp1MaxSize(num))
      .withConnectHandler(conn -> {
        sessionIds.add(ByteBufUtil.hexDump(conn.sslSession().getId()));
        connCount.incrementAndGet();
      })
      .build();
    CountDownLatch listenLatch = new CountDownLatch(1);
    vertx.deployVerticle(() -> new AbstractVerticle() {
      HttpServer server;
      @Override
      public void start(Promise<Void> startPromise) {
        server = vertx.createHttpServer(new HttpServerOptions()
          .setSsl(true)
          .setKeyStoreOptions(Cert.SERVER_JKS.get())
          .setHost(DEFAULT_HTTPS_HOST)
          .setPort(DEFAULT_HTTPS_PORT)
        ).requestHandler(req -> {
          HttpServerResponse resp = req.response().setChunked(true);
          expected.forEach(resp::write);
          resp.end();
        });
        server
          .listen(DEFAULT_HTTPS_PORT, DEFAULT_HTTPS_HOST)
          .<Void>mapEmpty()
          .onComplete(startPromise);
      }
    }, new DeploymentOptions().setInstances(num)).onComplete(onSuccess(v -> listenLatch.countDown()));
    awaitLatch(listenLatch);
    CountDownLatch connectionLatch = new CountDownLatch(num);
    for (int i = 0;i < num;i++) {
      client.request(HttpMethod.GET, DEFAULT_HTTPS_PORT, DEFAULT_HTTPS_HOST, DEFAULT_TEST_URI)
        .onComplete(onSuccess(request -> {
          connectionLatch.countDown();
        }));
      if (i == 0) {
        // Wait until the first connection is established to ensure other connections can reuse the session id
        waitUntil(() -> connectionLatch.getCount() == num - 1);
      }
    }
    awaitLatch(connectionLatch);
    assertEquals(num, sessionIds.size());
    assertEquals(1, new HashSet<>(sessionIds).size());
  }
}
