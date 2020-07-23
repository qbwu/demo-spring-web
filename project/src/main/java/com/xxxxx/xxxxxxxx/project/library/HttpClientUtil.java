/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/25 20:35
 */

package com.xxxxx.xxxxxxxx.project.library.hi;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpClientUtil {

    public static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static RequestConfig requestConfig;
    private static PoolingHttpClientConnectionManager connectionManager;
    private static int retryIntervalMs;

    @Autowired
    public HttpClientUtil(
        @Value("${http.client.con-req.timeout.ms:#{1000}}") int conReqTimeoutMs,
        @Value("${http.client.con.timeout.ms:#{3000}}") int conTimeoutMs,
        @Value("${http.client.socket.timeout.ms:#{15000}}") int soTimeoutMs,
        @Value("${http.client.con-pool.max:#{100}}") int conPoolMaxSize,
        @Value("${http.client.con-pool.per-route.max:#{20}}") int maxPerRoute,
        @Value("${http.client.retry.interval.ms:#{500}}") int retryIntervalMs) {

        HttpClientUtil.retryIntervalMs = retryIntervalMs;

        requestConfig = RequestConfig.custom().setConnectTimeout(conTimeoutMs).setSocketTimeout(soTimeoutMs)
                .setConnectionRequestTimeout(conReqTimeoutMs).build();

        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (Exception e) {
            logger.error("Failed to set sslContext, {}", e.getMessage(), e);
        }

        @SuppressWarnings("deprecation")
        LayeredConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext, new String[] {"TLSv1"}, null,
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslSocketFactory)
                        .register("http", new PlainConnectionSocketFactory()).build();
        connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(conPoolMaxSize);
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
    }

    private static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient =
                HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig)
                        .build();
        return httpClient;
    }

    private static String httpRequestRetry(HttpUriRequest httpRequest, String charset) throws Exception {
        String res = null;
        for (int i = 0; i < 3; i++) {
            try {
                res = httpRequest(httpRequest, charset);
            } catch (Exception e) {
                logger.warn("Retry times {}, error: {}", i, e.getMessage(), e);
                Thread.sleep(retryIntervalMs);
                continue;
            }
            break;
        }

        if (res == null) {
            throw new Exception("Http request retry failed");
        }
        return res;
    }

    private static String httpRequest(HttpUriRequest httpRequest, String charset) throws Exception {
        CloseableHttpResponse response = null;
        String responseContent = null;
        try {
            CloseableHttpClient httpClient = getHttpClient();
            response = httpClient.execute(httpRequest);
            int responseCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String resp = null;
            if (null != entity) {
                resp = EntityUtils.toString(entity, charset);
            }
            if (200 == responseCode) {
                responseContent = resp;
            } else {
                throw new HttpResponseException(responseCode, resp);
            }
        } finally {
            if (null != response) {
                response.close();
            }
        }
        return responseContent;
    }

    public String httpPost(String url, Map<String, String> paramsMap) throws Exception {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (paramsMap != null && !paramsMap.isEmpty()) {
            Set<String> paramKeys = paramsMap.keySet();
            for (String paramKey : paramKeys) {
                params.add(new BasicNameValuePair(paramKey, paramsMap.get(paramKey)));
            }
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Charsets.UTF_8);
        post.setEntity(entity);
        return httpRequestRetry(post, "UTF-8");
    }

    public String httpPost(String url, Map<String, String> paramsMap, Map<String, String> header) throws Exception {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (paramsMap != null && !paramsMap.isEmpty()) {
            Set<String> paramKeys = paramsMap.keySet();
            for (String paramKey : paramKeys) {
                params.add(new BasicNameValuePair(paramKey, paramsMap.get(paramKey)));
            }
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Charsets.UTF_8);
        post.setEntity(entity);
        addHeader(header, post);
        return httpRequestRetry(post, "UTF-8");
    }

    public String httpGet(String url, Map<String, String> paramsMap) throws Exception {
        String apiUrl = url;
        StringBuffer paramStr = new StringBuffer();
        if (paramsMap != null && !paramsMap.isEmpty()) {
            int i = 0;
            for (String key : paramsMap.keySet()) {
                if (i == 0) {
                    paramStr.append("?");
                } else {
                    paramStr.append("&");
                }
                paramStr.append(key).append("=").append(paramsMap.get(key));
                i++;
            }
            apiUrl += paramStr.toString();
        }

        HttpGet get = new HttpGet(apiUrl);
        return httpRequestRetry(get, "UTF-8");
    }

    public String httpPostJson(String url, String json, Map<String, String> header) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        addHeader(header, httpPost);
        StringEntity stringEntity = new StringEntity(json, "UTF-8");
        stringEntity.setContentEncoding("UTF-8");
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);

        return httpRequestRetry(httpPost, "UTF-8");
    }

    private static void addHeader(Map<String, String> headerMap, AbstractHttpMessage httpMessage) {
        if (headerMap != null && !headerMap.isEmpty()) {
            for (Map.Entry<String, String> me : headerMap.entrySet()) {
                httpMessage.setHeader(me.getKey(), me.getValue());
            }
        } else {
            httpMessage
                    .setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            httpMessage.setHeader("Accept-Language", "zh-CN,zh");
            httpMessage.setHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/39.0.2171.95 Safari/537.36");
        }
    }
}
