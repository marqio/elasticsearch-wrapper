package com.baidu.siem.utils;

import com.baidu.siem.client.HttpClientFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import org.apache.http.client.HttpClient;

/**
 * 封装了一些采用HttpClient发送HTTP请求的方法
 *
 * @create Feb 1, 2012 3:02:27 PM
 * @update Oct 8, 2012 3:48:55 PM
 */
public class HttpClientUtil {
    protected static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    /**
     * 发送java请求
     *
     * @param reqURL
     * @param requestData
     * @param method
     * @return
     * @throws IOException
     */
    public static String sendHttpRequestByHttpClient(String reqURL, String requestData, String contentType, String method) throws IOException {
        contentType = contentType == null || "".equals(contentType) ? "text/plain; charset=UTF-8" : contentType;
        if ("GET".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData))
                return sendGetRequest(reqURL, contentType, null);
            return sendPostRequest(reqURL, requestData, contentType, true);
        }
        if ("POST".equalsIgnoreCase(method)) {
            return sendPostRequest(reqURL, requestData, contentType, true);
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return sendPutRequest(reqURL, requestData, contentType, true);
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return sendDELETERequest(reqURL, contentType, null);
        }

        return "515,text/plain; charset=UTF-8|not supported http method currently";
    }

    private static String sendHeadRequest(String reqURL) throws IOException {
        String responseContent = null; // 响应内容
        HttpEntity entity = null;
        HttpClient httpClient = HttpClientFactory.getHttpClient();// 创建默认的httpClient实例
        try {
            HttpHead httpHead = new HttpHead(reqURL); // 创建org.apache.http.client.methods.HttpHead
            HttpResponse response = httpClient.execute(httpHead); // 执行Head请求
            return response.getStatusLine().getStatusCode() + "," + response.getFirstHeader("content-type").getValue() + "|" + responseContent;

        } catch (IOException e) {
            throw e;
        } finally {
//            httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
            try {
                EntityUtils.consume(entity); // Consume response content
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
     * 发送HTTP_GET请求
     *
     * @param reqURL        请求地址(含参数)
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     */
    public static String sendGetRequest(String reqURL, String contentType, String decodeCharset) throws IOException {

        String responseContent = null; // 响应内容
        HttpEntity entity = null;
        HttpClient httpClient = HttpClientFactory.getHttpClient();// 创建默认的httpClient实例
        try {
            HttpGet httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
            httpGet.setHeader(HTTP.CONTENT_TYPE, contentType);
            HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
            entity = response.getEntity(); // 获取响应实体
            if (null != entity) {
                responseContent = EntityUtils.toString(entity,
                        decodeCharset == null ? "UTF-8" : decodeCharset);
            }
            return response.getStatusLine().getStatusCode() + "," + response.getFirstHeader("content-type").getValue() + "|" + responseContent;

        } catch (IOException e) {
            throw e;
        } finally {
//            httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
            try {
                EntityUtils.consume(entity); // Consume response content
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * 发送HTTP_DELETE请求
     *
     * @param reqURL        请求地址(含参数)
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     */
    public static String sendDELETERequest(String reqURL, String contentType, String decodeCharset) throws IOException {

        String responseContent = null; // 响应内容
        HttpClient httpClient = HttpClientFactory.getHttpClient(); // 创建默认的httpClient实例
        HttpEntity entity = null;
        try {
            HttpDelete httpDelete = new HttpDelete(reqURL); // 创建org.apache.http.client.methods.HttpGet
            httpDelete.setHeader(HTTP.CONTENT_TYPE, contentType);
            HttpResponse response = httpClient.execute(httpDelete); // 执行DELETE请求
            entity = response.getEntity(); // 获取响应实体
            if (null != entity) {
                responseContent = EntityUtils.toString(entity,
                        decodeCharset == null ? "UTF-8" : decodeCharset);
            }
            return response.getStatusLine().getStatusCode() + "," + response.getFirstHeader("content-type").getValue() + "|" + responseContent;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                EntityUtils.consume(entity); // Consume response content
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * 发送HTTP_POST请求
     *
     * @param isEncoder 用于指明请求数据是否需要UTF-8编码,true为需要
     *                  //     * @see 该方法为<code>sendPostRequest(String,String,boolean,String,String)</code>
     *                  的简化方法
     *                  //     * @see 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8
     *                  //     * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
     *                  ]等特殊字符进行<code>URLEncoder.encode(string,"UTF-8")</code>
     */
    public static String sendPostRequest(String reqURL, String sendData, String contentType,
                                         boolean isEncoder) throws IOException {
        return sendPostRequest(reqURL, sendData, contentType, isEncoder, null, null);
    }

    /**
     * 发送HTTP_POST请求
     *
     * @param reqURL        请求地址
     * @param sendData      请求参数,若有多个参数则应拼接成param11=value11¶m22=value22¶m33=value33的形式后,
     *                      传入该参数中
     * @param isEncoder     请求数据是否需要encodeCharset编码,true为需要
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     * //     * @see 该方法会自动关闭连接,释放资源
     * //     * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
     * ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
     */
    public static String sendPostRequest(String reqURL, String sendData, String contentType,
                                         boolean isEncoder, String encodeCharset, String decodeCharset) throws IOException {
        String responseContent = null;
        HttpEntity entity = null;
        DefaultHttpClient httpClient = HttpClientFactory.getHttpClient();
        HttpPost httpPost = new HttpPost(reqURL);
        httpPost.setHeader(HTTP.CONTENT_TYPE, contentType);

        try {
            if (sendData != null && !"".equals(sendData.trim())) {
                if (isEncoder) {
                    httpPost.setEntity(new StringEntity(sendData, getCharSet(contentType)));
                } else {
                    httpPost.setEntity(new StringEntity(sendData));
                }
            }
            HttpResponse response = httpClient.execute(httpPost);
            entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity,
                        decodeCharset == null ? "UTF-8" : decodeCharset);
            }
            return response.getStatusLine().getStatusCode() + "," + response.getFirstHeader("content-type").getValue() + "|" + responseContent;

        } catch (Exception e) {
            throw e;
        } finally {
            try {
                EntityUtils.consume(entity); // Consume response content,close stream
            } catch (IOException e) {
                throw e;
            }
        }
    }

    private static String getCharSet(String contentType) {
        try {
            return contentType.indexOf("charset=") == -1 ? Charset.defaultCharset().toString() : contentType.substring(contentType.indexOf("charset=") + 8);
        } catch (Exception e) {
            return Charset.defaultCharset().toString();
        }

    }


    /**
     * 发送HTTP_PUT请求
     *
     * @param isEncoder 用于指明请求数据是否需要UTF-8编码,true为需要
     *                  //     * @see 该方法为<code>sendPostRequest(String,String,boolean,String,String)</code>
     *                  的简化方法
     *                  //     * @see 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8
     *                  //     * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
     *                  ]等特殊字符进行<code>URLEncoder.encode(string,"UTF-8")</code>
     */
    public static String sendPutRequest(String reqURL, String sendData, String contentType,
                                        boolean isEncoder) throws IOException {
        return sendPutRequest(reqURL, sendData, contentType, isEncoder, null, null);
    }

    /**
     * 发送HTTP_PUT请求
     *
     * @param reqURL        请求地址
     * @param sendData      请求参数,若有多个参数则应拼接成param11=value11¶m22=value22¶m33=value33的形式后,
     *                      传入该参数中
     * @param isEncoder     请求数据是否需要encodeCharset编码,true为需要
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     * //     * @see 该方法会自动关闭连接,释放资源
     * //     * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
     * ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
     */
    public static String sendPutRequest(String reqURL, String sendData, String contentType,
                                        boolean isEncoder, String encodeCharset, String decodeCharset) throws IOException {
        String responseContent = null;
        HttpClient httpClient = HttpClientFactory.getHttpClient();
        HttpPut httpPut = new HttpPut(reqURL);
        HttpEntity entity = null;
        httpPut.setHeader(HTTP.CONTENT_TYPE, contentType);
        try {
            if (sendData != null && (!"".equals(sendData.trim()))) {
                if (isEncoder) {
                    httpPut.setEntity(new StringEntity(sendData, getCharSet(contentType)));
                } else {
                    httpPut.setEntity(new StringEntity(sendData));
                }
            }
            HttpResponse response = httpClient.execute(httpPut);
            entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity,
                        decodeCharset == null ? "UTF-8" : decodeCharset);
            }
            return response.getStatusLine().getStatusCode() + "," + response.getFirstHeader("content-type").getValue() + "|" + responseContent;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                EntityUtils.consume(entity); // Consume response content,close stream
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * 发送java请求
     *
     * @param reqURL
     * @param requestData
     * @param method
     * @return
     * @throws IOException
     */
    public static String sendHttpRequestByJava(String reqURL, String requestData, String contentType, String method) throws IOException {
        contentType = contentType == null || "".equals(contentType) ? "text/plain; charset=UTF-8" : contentType;
        if ("GET".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData))
                return sendRequestByJavaWithoutBody(reqURL, contentType, "GET");
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "GET");
        }
        if ("POST".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData))
                return sendRequestByJavaWithoutBody(reqURL, contentType, "POST");
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "POST");
        }
        if ("PUT".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData))
                return sendRequestByJavaWithoutBody(reqURL, contentType, "PUT");
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "PUT");
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData))
                return sendRequestByJavaWithoutBody(reqURL, contentType, "DELETE");
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "DELETE");
        }

        return "512,text/plain; charset=UTF-8|not supported http method currently";
    }

    /**
     * 发送java_noBody请求
     *
     * @param reqURL 请求地址
     * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
     * 若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code>
     * //     * @see 若发送的<code>params</code>中含有中文,记得按照双方约定的字符集将中文
     * <code>URLEncoder.encode(string,encodeCharset)</code>
     * //     * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
     */
    public static String sendRequestByJavaWithoutBody(String reqURL, String contentType, String method) throws IOException {
        HttpURLConnection httpURLConnection = null;
        InputStream in = null; // 读
        int httpStatusCode = 0; // 远程主机响应的HTTP状态码
        try {
            URL sendUrl = new URL(reqURL);
            httpURLConnection = (HttpURLConnection) sendUrl.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setConnectTimeout(3000); // 30秒连接超时
            httpURLConnection.setReadTimeout(50000); // 30秒读取超时
            httpURLConnection.setRequestProperty(HTTP.CONTENT_TYPE, contentType);
            httpURLConnection.connect();

            // 获取HTTP状态码
            httpStatusCode = httpURLConnection.getResponseCode();
            in = httpURLConnection.getInputStream();

            String charset = httpURLConnection.getContentEncoding() == null ? Charset.defaultCharset().toString() : httpURLConnection.getContentEncoding();
            return httpStatusCode + "," + httpURLConnection.getContentType() + "|" + readLineString(in, charset);
        } catch (IOException e) {
            return sendHttpRequestByHttpClient(reqURL, null, contentType, method);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    /**
     * 发送java_body请求
     *
     * @param reqURL 请求地址
     * @param params 发送到远程主机的正文数据,其数据类型为<code>java.util.Map<String, String></code>
     * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
     * 若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code>
     * //     * @see 若发送的<code>params</code>中含有中文,记得按照双方约定的字符集将中文
     * <code>URLEncoder.encode(string,encodeCharset)</code>
     * //     * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
     */
    public static String sendPostRequestByJava(String reqURL,
                                               Map<String, String> params, String contentType, String method) throws IOException {
        StringBuilder sendData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sendData.append(entry.getKey()).append("=")
                    .append(entry.getValue()).append("&");
        }
        if (sendData.length() > 0) {
            sendData.setLength(sendData.length() - 1); // 删除最后一个&符号
        }

        return sendRequestByJavaWithBody(reqURL, sendData.toString(), contentType, method);
    }

    /**
     * 发送java_body请求
     *
     * @param reqURL   请求地址
     * @param sendData 发送到远程主机的正文数据
     * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
     * 若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code>
     * //     * @see 若发送的<code>sendData</code>中含有中文,记得按照双方约定的字符集将中文
     * <code>URLEncoder.encode(string,encodeCharset)</code>
     * //     * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
     */
    public static String sendRequestByJavaWithBody(String reqURL, String sendData, String contentType, String method) throws IOException {
        HttpURLConnection httpURLConnection = null;
        OutputStream out = null; // 写
        InputStream in = null; // 读
        int httpStatusCode = 0; // 远程主机响应的HTTP状态码
        try {
            URL sendUrl = new URL(reqURL);
            httpURLConnection = (HttpURLConnection) sendUrl.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty(HTTP.CONTENT_TYPE, contentType);
            httpURLConnection.setDoOutput(true); // 指示应用程序要将数据写入URL连接,其值默认为false
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setConnectTimeout(3000); // 50秒建立连接超时
            httpURLConnection.setReadTimeout(50000); // 50秒读取超时
            out = httpURLConnection.getOutputStream();
            out.write(sendData.toString().getBytes(getCharSet(contentType)));
            // 清空缓冲区,发送数据
            out.flush();
            // 获取HTTP状态码
            httpStatusCode = httpURLConnection.getResponseCode();

            in = httpURLConnection.getInputStream();
            String charset = httpURLConnection.getContentEncoding() == null ? Charset.defaultCharset().toString() : httpURLConnection.getContentEncoding();
            return httpStatusCode + "," + httpURLConnection.getContentType() + "|" + readLineString(in, charset);

        } catch (IOException e) {
            return sendHttpRequestByHttpClient(reqURL, sendData, contentType, method);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    public static String readLineString(InputStream inputStream, String charset) throws IOException {
        String res = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        String str;
        while ((str = reader.readLine()) != null) {
            res += str + "\n";
        }

        return res;
    }

    /**
     * 不可靠，消息体过长会丢失
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String readBytesString(InputStream inputStream) throws IOException {
        byte[] byteDatas = new byte[inputStream.available()];
        int length = 0;
        inputStream.read(byteDatas);
        while ((length = inputStream.available()) > 0) {
            byte[] array = byteDatas;
            byteDatas = new byte[array.length + length];
            System.arraycopy(array, 0, byteDatas, 0, array.length);
            inputStream.read(byteDatas, array.length, length);
        }

        return new String(byteDatas);
    }


    /**
     * 发送HTTPS_POST请求
     * <p/>
     * //     * @see 该方法为<code>sendPostSSLRequest(String,Map<String,String>,String,String)</code>
     * 方法的简化方法
     * //     * @see 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8
     * //     * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
     * <code>URLEncoder.encode(string,"UTF-8")</code>
     */
    public static String sendPostSSLRequest(String reqURL,
                                            Map<String, String> params) {
        return sendPostSSLRequest(reqURL, params, null, null);
    }


    /**
     * 发送HTTPS_POST请求
     *
     * @param reqURL        请求地址
     * @param params        请求参数
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     * //     * @see 该方法会自动关闭连接,释放资源
     * //     * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
     * <code>URLEncoder.encode(string,encodeCharset)</code>
     */
    public static String sendPostSSLRequest(String reqURL,
                                            Map<String, String> params, String encodeCharset,
                                            String decodeCharset) {
        String responseContent = "";
        HttpClient httpClient = new DefaultHttpClient();
        X509TrustManager xtm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{xtm}, null);
            SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
            httpClient.getConnectionManager().getSchemeRegistry()
                    .register(new Scheme("https", 443, socketFactory));

            HttpPost httpPost = new HttpPost(reqURL);
            List<NameValuePair> formParams = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formParams.add(new BasicNameValuePair(entry.getKey(), entry
                        .getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(formParams,
                    encodeCharset == null ? "UTF-8" : encodeCharset));

            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity,
                        decodeCharset == null ? "UTF-8" : decodeCharset);
                EntityUtils.consume(entity);
            }
        } catch (Exception e) {
            return "Http PUT Communication error msg:\n" + e.getMessage();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return responseContent;
    }
}