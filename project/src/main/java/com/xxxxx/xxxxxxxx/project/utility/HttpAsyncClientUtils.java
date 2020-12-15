/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/11/04 17:39
 */

package com.xxxxx.xxxxxxxx.project.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@Component
public class HttpAsyncClientUtils {

    public static final Logger logger = LoggerFactory.getLogger(HttpAsyncClientUtils.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static CloseableHttpAsyncClient httpAsyncClient;

    @Autowired
    public HttpAsyncClientUtils(
            @Value("${http.async.client.con-req.timeout.ms:#{1000}}") int conReqTimeoutMs,
            @Value("${http.async.client.con.timeout.ms:#{3000}}") int conTimeoutMs,
            @Value("${http.async.client.socket.timeout.ms:#{15000}}") int soTimeoutMs,
            @Value("${http.async.client.con-pool.max:#{100}}") int conPoolMaxSize,
            @Value("${http.async.client.con-pool.per-route.max:#{20}}") int maxPerRoute) throws IOReactorException {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(conTimeoutMs)
                .setSocketTimeout(soTimeoutMs)
                .setConnectionRequestTimeout(conReqTimeoutMs).build();

        RegistryBuilder<SchemeIOSessionStrategy> schemeIOSessionStrategyRegistryBuilder
                = RegistryBuilder.<SchemeIOSessionStrategy>create()
                                    .register("http", NoopIOSessionStrategy.INSTANCE);
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null,
                    // Trust all
                    (TrustStrategy) (chain, authType) -> true).build();

            SSLIOSessionStrategy sslioSessionStrategy = new SSLIOSessionStrategy(
                    sslContext, new String[] {"TLSv1"}, null, NoopHostnameVerifier.INSTANCE);

            schemeIOSessionStrategyRegistryBuilder.register("https", sslioSessionStrategy);
        } catch (Exception e) {
            logger.error("Failed to set sslContext, {}", e.getMessage(), e);
        }

        PoolingNHttpClientConnectionManager connectionManager =
                new PoolingNHttpClientConnectionManager(
                        new DefaultConnectingIOReactor(IOReactorConfig.DEFAULT, null),
                        schemeIOSessionStrategyRegistryBuilder.build());
        connectionManager.setMaxTotal(conPoolMaxSize);
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        httpAsyncClient = HttpAsyncClients.custom().setConnectionManager(connectionManager)
                                .setConnectionManagerShared(false)
                                .setDefaultRequestConfig(requestConfig).build();
        httpAsyncClient.start();
    }

    @PreDestroy
    public void close() throws IOException {
        httpAsyncClient.close();
    }

    public Future<HttpResponse> httpPost(String url, Map<String, String> paramsMap,
                                         Map<String, String> header,
                                         FutureCallback<HttpResponse> callback) {
        HttpPost post = new HttpPost(url);
        HttpClientUtils.addUrlEncodedFormEntity(paramsMap, post);
        HttpClientUtils.addHeader(header, post);
        return httpAsyncClient.execute(post, callback);
    }

    public Future<HttpResponse> httpPostJson(String url, Map<String, Object> json,
            Map<String, String> header, FutureCallback<HttpResponse> callback)
                throws JsonProcessingException {
        HttpPost post = new HttpPost(url);
        HttpClientUtils.addHeader(header, post);
        HttpClientUtils.addJsonEntity(json, post);
        return httpAsyncClient.execute(post, callback);
    }

    abstract class AbstractAsyncRequestHttpEntity extends BasicHttpEntity
            implements HttpAsyncContentProducer { }

    public Future<HttpResponse> httpPostJson(String url,
            Supplier<Map<String, Object>> jsonProducer, Map<String, String> header,
            FutureCallback<HttpResponse> callback) {

        HttpPost post = new HttpPost(url);
        HttpClientUtils.addHeader(header, post);
        post.setEntity(new AbstractAsyncRequestHttpEntity() {
            { setContentType(ContentType.APPLICATION_JSON.toString()); }

            @Override
            public void close() {
            }

            @Override
            public void produceContent(ContentEncoder encoder, IOControl ioControl) throws IOException {
                Map<String, Object> json = jsonProducer.get();
                String content = jsonMapper.writeValueAsString(json);
                NStringEntity entity = new NStringEntity(content, ContentType.APPLICATION_JSON);
                entity.produceContent(encoder, ioControl);
                entity.close();
            }
        });

        return httpAsyncClient.execute(
                HttpAsyncMethods.create(URIUtils.extractHost(post.getURI()), post),
                HttpAsyncMethods.createConsumer(),
                HttpClientContext.create(),
                callback);
    }
}
