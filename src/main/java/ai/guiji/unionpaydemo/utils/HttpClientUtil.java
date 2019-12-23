package ai.guiji.unionpaydemo.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * author: zhouliliang
 * Date: 2019/12/23 11:22
 * Description:
 */
public class HttpClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    public static final String DEFAULT_ENCODING = "UTF-8";// 默认编码

    private static HttpClientUtil instance;

    static {
        try {
            instance = new HttpClientUtil();
        } catch (KeyManagementException e) {
            logger.error("http client 初始化失败");
        }
    }

    private static HttpClient client;

    private static long startTime = System.currentTimeMillis();

    private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {

        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            long keepAlive = super.getKeepAliveDuration(response, context);

            if (keepAlive == -1) {
                keepAlive = 5000;
            }
            return keepAlive;
        }

    };

    private HttpClientUtil() throws KeyManagementException {
        int httpReqTimeOut = 60000;//60秒

        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // 初始化SSL上下文
        sslContext.init(null, new TrustManager[]{tm}, new SecureRandom());
        // SSL套接字连接工厂,NoopHostnameVerifier为信任所有服务器
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        // 注册http套接字工厂和https套接字工厂
        Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslsf)
                .build();
        // 连接池管理器
        PoolingHttpClientConnectionManager pcm = new PoolingHttpClientConnectionManager(r);
        pcm.setMaxTotal(100);//连接池最大连接数
        pcm.setDefaultMaxPerRoute(100);//每个路由最大连接数
        /**
         *  请求参数配置
         *  connectionRequestTimeout:
         *                          从连接池中获取连接的超时时间，超过该时间未拿到可用连接，
         *                          会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
         *  connectTimeout:
         *                  连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
         *  socketTimeout:
         *                  服务器返回数据(response)的时间，超过该时间抛出read timeout
         */
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(httpReqTimeOut)
                .setConnectTimeout(httpReqTimeOut)
                .setSocketTimeout(httpReqTimeOut)
                .build();

//        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
//            @Override
//            public boolean retryRequest(
//                    IOException exception,
//                    int executionCount,
//                    HttpContext context) {
//                if (executionCount >= 5) {
//                    // Do not retry if over max retry count
//                    return false;
//                }
//                if (exception instanceof InterruptedIOException) {
//                    // Timeout
//                    return false;
//                }
//                if (exception instanceof UnknownHostException) {
//                    // Unknown host
//                    return false;
//                }
//                if (exception instanceof ConnectTimeoutException) {
//                    // Connection refused
//                    return false;
//                }
//                if (exception instanceof SSLException) {
//                    // SSL handshake exception
//                    return false;
//                }
//                HttpClientContext clientContext = HttpClientContext.adapt(context);
//                HttpRequest request = clientContext.getRequest();
//                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
//                if (idempotent) {
//                    // Retry if the request is considered idempotent
//                    return true;
//                }
//                return false;
//            }
//        };

//        ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy = new ServiceUnavailableRetryStrategy() {
//            /**
//             * retry逻辑
//             */
//            @Override
//            public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
//                if (executionCount <= 3)
//                    return true;
//                else
//                    return false;
//            }
//
//            /**
//             * retry间隔时间
//             */
//            @Override
//            public long getRetryInterval() {
//                return 200;
//            }
//        };

        /**
         * 构造closeableHttpClient对象
         */
        client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(pcm)
                .setKeepAliveStrategy(keepAliveStrat)
//                .setRetryHandler(retryHandler)
//                .setServiceUnavailableRetryStrategy(serviceUnavailableRetryStrategy)
                .build();
    }

    public static HttpClientUtil getInstance() {
        return instance;
    }

    public static HttpClient getHttpClient() {
        return client;
    }

    public static String get(String url, Map<String, String> params) {
        return get(url, params, DEFAULT_ENCODING);
    }

    public static String get(String url, Map<String, String> params, String encoding) {
        String result = "";
        if (Objects.isNull(url) || "".equals(url)) {
            logger.info("----->url为空");
            return result;
        }

        HttpClient httpClient = getHttpClient();
        HttpGet httpGet = null;
        List<NameValuePair> nameValuePairs = convertMap2PostParams(params);
        HttpResponse response = null;
        try {
            if (nameValuePairs != null && nameValuePairs.size() > 0) {
                URIBuilder builder = new URIBuilder(url);
                builder.setParameters(nameValuePairs);
                httpGet = new HttpGet(builder.build());
            } else {
                httpGet = new HttpGet(url);
            }

            // 发送请求，并接收响应
            response = httpClient.execute(httpGet);
            result = handleResponse(url, encoding, response);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String post(String url, Map<String, String> reqMap) {
        return post(url, reqMap, DEFAULT_ENCODING);
    }

    public static String post(String url, Map<String, String> params, String encoding) {
        String result = "";
        if (Objects.isNull(url) || "".equals(url)) {
            logger.info("----->url为空");
            return result;
        }

        HttpClient httpClient = getHttpClient();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> valuePairs = convertMap2PostParams(params);
        UrlEncodedFormEntity urlEncodedFormEntity = null;
        HttpResponse response = null;
        try {
            urlEncodedFormEntity = new UrlEncodedFormEntity(valuePairs, encoding);
            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            httpPost.setEntity(urlEncodedFormEntity);
            // 发送请求，并接收响应
            response = httpClient.execute(httpPost);
            result = handleResponse(url, encoding, response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static List<NameValuePair> convertMap2PostParams(Map<String, String> params) {
        List<String> keys = new ArrayList<String>(params.keySet());
        if (keys.isEmpty()) {
            return null;
        }

        int keySize = keys.size();
        List<NameValuePair> pairs = new LinkedList<NameValuePair>();
        for (int i = 0; i < keySize; i++) {
            String key = keys.get(i);
            String value = params.get(key);
            pairs.add(new BasicNameValuePair(key, value));
        }

        return pairs;
    }

    /**
     * 处理响应，获取响应报文
     *
     * @param url
     * @param encoding
     * @param response
     * @return
     * @throws IOException
     */
    private static String handleResponse(String url, String encoding, HttpResponse response) {
        String content = null;

        try {
            if (Objects.nonNull(response)) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    // 获取响应实体
                    HttpEntity entity = response.getEntity();

                    if (Objects.nonNull(entity)) {
                        Charset charset = ContentType.getOrDefault(entity).getCharset();
                        content = EntityUtils.toString(entity, charset);
                        // 释放entity
                        EntityUtils.consume(entity);
                    }
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    logger.error("-----> 请求404, 未找到资源. url:{}", url);
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    logger.error("-----> 请求500, 服务器端异常. url:{}", url);
                } else {
                    logger.error("-----> 请求statusCode:{}, url:{}", response.getStatusLine().getStatusCode(), url);
                }
            }
        } catch (Exception e) {
            logger.error("----->url: {}, 处理响应，获取响应报文异常：{}", url, e.getMessage());
        }

        return content;
    }

}
