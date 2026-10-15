/*
 * Copyright 2014 Sony Corporation
 */

package glasscam.cam.apis.sonyremote.sonystuff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import glasscam.utils.Log;

/**
 * Simple HTTP Client for sample application.
 */
public final class SimpleHttpClient {

    private static final String TAG = SimpleHttpClient.class.getSimpleName();

    private static final int DEFAULT_CONNECTION_TIMEOUT = 15000; // [msec]

    private static final int DEFAULT_READ_TIMEOUT = 15000; // [msec]

    private SimpleHttpClient() {

    }

    /**
     * This app is allowed cleartext HTTP app-wide (see network_security_config.xml -
     * the target camera's IP isn't known ahead of time, so it can't be scoped there).
     * This is the actual enforcement point instead: refuse to send a Sony remote-camera
     * request anywhere that isn't a private/link-local/loopback address, so a
     * misconfigured or malicious "camera IP" can't make this client leak the request
     * (auth token in the URL, etc.) to a host out on the open internet.
     */
    private static boolean isAllowedHost(URL url) {
        try {
            InetAddress addr = InetAddress.getByName(url.getHost());
            return addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                    || addr.isLoopbackAddress() || isIpv6UniqueLocalAddress(addr);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * InetAddress#isSiteLocalAddress() only recognizes the old, deprecated IPv6
     * site-local range (fec0::/10, RFC 3879, deprecated in 2004). It does not
     * recognize the modern Unique Local Address range (fc00::/7, RFC 4193) that
     * real IPv6 home/local networks actually use today. Without this check, a
     * legitimate local-network camera reachable only over IPv6 ULA would be
     * incorrectly refused by isAllowedHost() above - fails closed (no security
     * hole), but breaks a real use case. ULA is identified by the address's first
     * byte having its high bit set to 1 and the next-highest bit either value,
     * i.e. the top 7 bits equal 0b1111110 - equivalently, (firstByte & 0xFE) == 0xFC.
     */
    private static boolean isIpv6UniqueLocalAddress(InetAddress addr) {
        if (!(addr instanceof Inet6Address)) {
            return false;
        }
        byte firstByte = addr.getAddress()[0];
        return (firstByte & 0xFE) == 0xFC;
    }


    /**
     * Send HTTP GET request to the indicated url. Then returns response as
     * string.
     *
     * @param url request target
     * @return response as string
     * @throws IOException all errors and exception are wrapped by this
     *             Exception.
     */
    public static String httpGet(String url) throws IOException {
        HttpURLConnection httpConn = null;
        InputStream inputStream = null;

        // Open connection and input stream
        try {
            URL urlObj = new URL(url);
            if (!isAllowedHost(urlObj)) {
                Log.w(TAG, "httpGet: refusing non-local host: " + url);
                throw new IOException("Refusing cleartext request to a non-local host");
            }
            httpConn = (HttpURLConnection) urlObj.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
            httpConn.setReadTimeout(SimpleHttpClient.DEFAULT_READ_TIMEOUT);
            httpConn.connect();

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = httpConn.getInputStream();
            }
            if (inputStream == null) {
                Log.w(TAG, "httpGet: Response Code Error: " + responseCode + ": " + url);
                throw new IOException("Response Error:" + responseCode);
            }
        } catch (SocketTimeoutException e) {
            Log.w(TAG, "httpGet: Timeout: " + url);
            throw new IOException();
        } catch (MalformedURLException e) {
            Log.w(TAG, "httpGet: MalformedUrlException: " + url);
            throw new IOException();
        } catch (IOException e) {
            Log.w(TAG, "httpGet: " + e.getMessage());
            if (httpConn != null) {
                httpConn.disconnect();
            }
            throw e;
        }

        // Read stream as String
        BufferedReader reader = null;
        try {
            StringBuilder responseBuf = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            int c;
            while ((c = reader.read()) != -1) {
                responseBuf.append((char) c);
            }
            return responseBuf.toString();
        } catch (IOException e) {
            Log.w(TAG, "httpGet: read error: " + e.getMessage());
            throw e;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "IOException while closing BufferedReader");
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "IOException while closing InputStream");
            }
        }
    }

    /**
     * Send HTTP POST request to the indicated url. Then returns response as
     * string.
     *
     * @param url request target
     * @param postData POST body data as string (ex. JSON)
     * @return response as string
     * @throws IOException all errors and exception are wrapped by this
     *             Exception.
     */
    public static String httpPost(String url, String postData) throws IOException {
        return httpPost(url, postData, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Send HTTP POST request to the indicated url. Then returns response as
     * string.
     *
     * @param url request target
     * @param postData POST body data as string (ex. JSON)
     * @param timeout Request timeout
     * @return response as string
     * @throws IOException all errors and exception are wrapped by this
     *             Exception.
     */
    public static String httpPost(String url, String postData, int timeout) throws IOException {
        HttpURLConnection httpConn = null;
        OutputStream outputStream = null;
        OutputStreamWriter writer = null;
        InputStream inputStream = null;

        // Open connection and input stream
        try {
            URL urlObj = new URL(url);
            if (!isAllowedHost(urlObj)) {
                Log.w(TAG, "httpPost: refusing non-local host: " + url);
                throw new IOException("Refusing cleartext request to a non-local host");
            }
            httpConn = (HttpURLConnection) urlObj.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
            httpConn.setReadTimeout(timeout);
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            outputStream = httpConn.getOutputStream();
            writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            writer.write(postData);
            writer.flush();
            writer.close();
            writer = null;
            outputStream.close();
            outputStream = null;

            int responseCode = httpConn.getResponseCode();
            //Log.d(TAG, "httpPost: Response Code: " + responseCode + ": " + url);
            //if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = httpConn.getInputStream();
            //}
            if (inputStream == null) {
                Log.w(TAG, "httpPost: Response Code Error: " + responseCode + ": " + url);
                throw new IOException("Response Error:" + responseCode);
            }
        } catch (SocketTimeoutException e) {
            Log.w(TAG, "httpPost: Timeout: " + url);
            throw new IOException();
        } catch (MalformedURLException e) {
            Log.w(TAG, "httpPost: MalformedUrlException: " + url);
            throw new IOException();
        } catch (IOException e) {
            Log.w(TAG, "httpPost: IOException: " + e.getMessage());
            if (httpConn != null) {
                httpConn.disconnect();
            }
            throw e;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "IOException while closing OutputStreamWriter");
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "IOException while closing OutputStream");
            }
        }

        // Read stream as String
        BufferedReader reader = null;
        try {
            StringBuilder responseBuf = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            int c;
            while ((c = reader.read()) != -1) {
                responseBuf.append((char) c);
            }
            return responseBuf.toString();
        } catch (IOException e) {
            Log.w(TAG, "httpPost: read error: " + e.getMessage());
            throw e;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "IOException while closing BufferedReader");
            }
        }
    }
}
