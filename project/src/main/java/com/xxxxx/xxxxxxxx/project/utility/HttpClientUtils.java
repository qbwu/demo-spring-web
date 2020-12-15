/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/25 20:35
 */

package com.xxxxx.xxxxxxxx.project.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.Charsets;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
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
public class HttpClientUtils implements com.xxxxx.xxxxxxxx.file.utility.HttpClientUtils {

    private static Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    private static int retryIntervalMs;

    private static CloseableHttpClient httpClient;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    public HttpClientUtils(
        @Value("${http.client.con-req.timeout.ms:#{1000}}") int conReqTimeoutMs,
        @Value("${http.client.con.timeout.ms:#{3000}}") int conTimeoutMs,
        @Value("${http.client.socket.timeout.ms:#{15000}}") int soTimeoutMs,
        @Value("${http.client.con-pool.max:#{100}}") int conPoolMaxSize,
        @Value("${http.client.con-pool.per-route.max:#{20}}") int maxPerRoute,
        @Value("${http.client.retry.interval.ms:#{500}}") int retryIntervalMs) {

        HttpClientUtils.retryIntervalMs = retryIntervalMs;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(conTimeoutMs)
                .setSocketTimeout(soTimeoutMs)
                .setConnectionRequestTimeout(conReqTimeoutMs).build();

        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory());
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null,
                    // Trust all
                    (TrustStrategy) (chain, authType) -> true).build();

            LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext, new String[] {"TLSv1"}, null, NoopHostnameVerifier.INSTANCE);

            socketFactoryRegistryBuilder.register("https", sslSocketFactory);
        } catch (Exception e) {
            logger.error("Failed to set sslContext, {}", e.getMessage(), e);
        }

        PoolingHttpClientConnectionManager connectionManager
                = new PoolingHttpClientConnectionManager(socketFactoryRegistryBuilder.build());
        connectionManager.setMaxTotal(conPoolMaxSize);
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);

        httpClient = HttpClients.custom().setConnectionManager(connectionManager)
                                         .setConnectionManagerShared(false)
                                         .setDefaultRequestConfig(requestConfig).build();
    }

    @PreDestroy
    public void close() throws IOException {
        httpClient.close();
    }

    private static String httpRequestRetry(HttpUriRequest httpRequest, String charset) throws Exception {
        String res = null;
        
        int code = -1;
        String reason = null;
        
        for (int i = 0; i < 3; i++) {
            try {
                res = httpRequest(httpRequest, charset);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                Thread.sleep(50);
                if (e instanceof HttpResponseException) {
                    HttpResponseException httpResponseException = (HttpResponseException) e;
                    code = httpResponseException.getStatusCode();
                    reason = httpResponseException.getReasonPhrase();
                }
                continue;
            }
            break;
        }

        if (res == null) {
            logger.error("http request retry 3 times failed, last response code: {}, reason: {}",
                         code, reason);
            throw new HttpRetryException("http request all retries failed", code);
        }
        return res;
    }

    private static String httpRequest(HttpUriRequest httpRequest, String charset) throws Exception {
        CloseableHttpResponse response = null;
        String responseContent = null;
        try {
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

    @Override
    public String httpPost(String url, Map<String, String> paramsMap,
                           Map<String, String> header) throws Exception {
        HttpPost post = new HttpPost(url);
        addUrlEncodedFormEntity(paramsMap, post);
        addHeader(header, post);
        return httpRequestRetry(post, "UTF-8");
    }

    @Override
    public String httpPostJson(String url, String json, Map<String, String> header) throws Exception {
        HttpPost post = new HttpPost(url);
        addHeader(header, post);
        addJsonEntity(json, post);
        return httpRequestRetry(post, "UTF-8");
    }

    public String httpPostJson(String url, Map<String, Object> json,
                               Map<String, String> header) throws Exception {
        HttpPost post = new HttpPost(url);
        addHeader(header, post);
        addJsonEntity(json, post);
        return httpRequestRetry(post, "UTF-8");
    }

    @Override
    public String httpGet(String url, Map<String, String> paramsMap,
                          Map<String, String> header) throws Exception {
        HttpGet get = new HttpGet(addQueryStr(paramsMap, url));
        addHeader(header, get);
        return httpRequestRetry(get, "UTF-8");
    }

    // TODO refactor these codes
    @Override
    public String httpPostForm(String url, Part[] parts, Map<String, String> headerMap) throws Exception {
        PostMethod postMethod = new PostMethod(url);
        if (headerMap != null && !headerMap.isEmpty()) {
            for (Map.Entry<String, String> me : headerMap.entrySet()) {
                postMethod.addRequestHeader(me.getKey(), me.getValue());
            }
        }
        MultipartRequestEntity mre = new MultipartRequestEntity(parts, postMethod.getParams());
        postMethod.setRequestEntity(mre);
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams() .setConnectionTimeout(60000);
        int status = client.executeMethod(postMethod);
        if (status == HttpStatus.SC_OK) {
            InputStream inputStream = postMethod.getResponseBodyAsStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer stringBuffer = new StringBuffer();
            String str;
            while ((str = br.readLine()) != null) {
                stringBuffer.append(str);
            }
            return stringBuffer.toString();
        }
        logger.error("http post form error. status={}. postMethod={}", status, postMethod);
        throw new HttpResponseException(status, "http post form response error");
    }

    // TODO refactor these codes
    @Override
    public void httpPostDownload(String url, Map<String, String> data,
                                 OutputStream outputStream, Map<String, String> header) throws Exception {
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> me : header.entrySet()) {
                postMethod.addRequestHeader(me.getKey(), me.getValue());
            }
        }
        for (Map.Entry<String, String> m : data.entrySet()) {
            postMethod.addParameter(m.getKey(), m.getValue());
        }

        int status = client.executeMethod(postMethod);
        if (status == HttpStatus.SC_OK) {
            InputStream inputStream = postMethod.getResponseBodyAsStream();
            int n;
            byte[] bytes = new byte[4096];
            while (-1 != (n = inputStream.read(bytes))) {
                outputStream.write(bytes, 0, n);
            }
        }

        logger.error("file download failed. status={}. postMethod={}", status, postMethod);
        throw new HttpResponseException(status, "file download response error");
    }

    static void addHeader(Map<String, String> headerMap, AbstractHttpMessage httpMessage) {
        if (headerMap == null) {
            return;
        }
        for (Map.Entry<String, String> me : headerMap.entrySet()) {
            httpMessage.setHeader(me.getKey(), me.getValue());
        }
        httpMessage.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        httpMessage.setHeader("Accept-Language", "zh-CN,zh");
        httpMessage.setHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/39.0.2171.95 Safari/537.36");
    }

    static void addUrlEncodedFormEntity(Map<String, String> paramsMap,
                                        HttpEntityEnclosingRequest httpRequest) {
        if (paramsMap == null || paramsMap.isEmpty()) {
            return;
        }
        List<NameValuePair> params = new ArrayList<>();
        Set<String> paramKeys = paramsMap.keySet();
        for (String paramKey : paramKeys) {
            params.add(new BasicNameValuePair(paramKey, paramsMap.get(paramKey)));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Charsets.UTF_8);
        httpRequest.setEntity(entity);
    }

    static String addQueryStr(Map<String, String> paramsMap, String url) {
        if (paramsMap == null || paramsMap.isEmpty()) {
            return url;
        }
        StringBuilder paramStr = new StringBuilder();
        paramStr.append(url);
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
        return paramStr.toString();
    }

    static void addJsonEntity(String jsonStr, HttpEntityEnclosingRequest httpRequest) {
        StringEntity stringEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpRequest.setEntity(stringEntity);
    }

    static void addJsonEntity(Map<String, Object> json, HttpEntityEnclosingRequest httpRequest)
            throws JsonProcessingException {
        StringEntity stringEntity = new StringEntity(jsonMapper.writeValueAsString(json),
                ContentType.APPLICATION_JSON);
        httpRequest.setEntity(stringEntity);
    }
}
