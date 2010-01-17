package com.yoursway.crashkit.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.yoursway.crashkit.internal.model.ClientCredentials;
import com.yoursway.crashkit.internal.utils.RequestString;
import com.yoursway.crashkit.internal.utils.YsFileUtils;

public class ServerConnection {
    
    private final String host;
    
    private final int port;
    
    @SuppressWarnings("unused")
    private final String productName;
    
    @SuppressWarnings("unused")
    private final String accountName;
    
    private final String productUrl;
    
    public ServerConnection(String accountName, String productName) {
        if (accountName == null)
            throw new NullPointerException("accountName is null");
        if (productName == null)
            throw new NullPointerException("productName is null");
        
        String host = determineHost(accountName);
        if (host.contains(":")) {
            this.host = host.substring(0, host.lastIndexOf(':'));
            this.port = Integer.parseInt(host.substring(host.lastIndexOf(':') + 1));
        } else {
            this.host = host;
            this.port = 80;
        }
        this.accountName = accountName;
        this.productName = productName;
        this.productUrl = "/" + accountName + "/products/" + productName;
    }
    
    public ClientCredentials obtainNewClientIdAndCookie() throws IOException {
        Map<String, String> result = get(obtainClientInfoUrl());
        String id = result.get("client_id");
        String cookie = result.get("client_cookie");
        if (id == null || cookie == null)
            throw new IOException("Invalid response: missing id and/or cookie");
        return new ClientCredentials(id, cookie);
    }
    
    public void sendReport(ClientCredentials clientCredentials, String payload, Collection<String> blobsToSend)
            throws IOException {
        System.out.println("Trying to send bug report to " + host + "...");
        URL url = postReportUrl(clientCredentials);
        Map<String, String> response = post(url, payload);
        String blobs = response.get("blobs");
        System.out.println("Report sent.");
        if (blobs != null)
            blobsToSend.addAll(Arrays.asList(blobs.split(",")));
    }
    
    public void sendBlob(ClientCredentials clientCredentials, String blob, String body) throws IOException {
        System.out.println("Trying to send an attachment to " + host + "...");
        URL url = postBlobUrl(clientCredentials, blob);
        post(url, body);
        System.out.println("Attachment sent.");
    }
    
    private Map<String, String> get(URL url) throws IOException {
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setUseCaches(false);
        return interpretResponse(urlConn);
    }
    
    private Map<String, String> post(URL url, String payload) throws IOException {
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream out = urlConn.getOutputStream();
        YsFileUtils.writeString(out, payload);
        out.flush();
        out.close();
        return interpretResponse(urlConn);
    }
    
    private Map<String, String> interpretResponse(HttpURLConnection urlConn) throws IOException,
            ServerInitiatedError {
        int code = urlConn.getResponseCode();
        if (code < 200 || code >= 300) {
            InputStream in = urlConn.getErrorStream();
            String response = null;
            if (in != null) {
                try {
                    Map<String, String> data = RequestString.decode(YsFileUtils.readAsString(in));
                    in.close();
                    response = data.get("response");
                } catch (IllegalArgumentException e) {
                }
            }
            if (response == null)
                throw new IOException("Response indicated failure: code " + code);
            else
                throw new ServerInitiatedError(response);
        } else {
            InputStream in = urlConn.getInputStream();
            String result = YsFileUtils.readAsString(in);
            in.close();
            Map<String, String> data = RequestString.decode(result);
            String response = data.get("response");
            if (response == null)
                throw new IOException("Server returned code 200, but no API response");
            if (!response.equalsIgnoreCase("ok"))
                throw new ServerInitiatedError(response);
            return data;
        }
    }
    
    private static String determineHost(String accountName) {
        String override = System.getProperty(accountName.replaceAll("[^a-zA-Z0-9.]+", "").toLowerCase()
                + ".feedback.host");
        if (override != null)
            return override;
        override = System.getenv(accountName.replaceAll("[^a-zA-Z0-9]+", "_").toUpperCase()
                + "_FEEDBACK_HOST");
        if (override != null && override.trim().length() > 0)
            return override;
        else
            return "crashkitapp.appspot.com";
    }
    
    public URL feedbackPageUrl() {
        try {
            return new URL("http", host, port, productUrl + "/feedback/");
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
    
    public URL postReportUrl(ClientCredentials clientCredentials) {
        try {
            return new URL("http", host, port, productUrl + "/post-report/" + clientCredentials.id() + "/"
                    + clientCredentials.cookie());
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
    
    public URL postBlobUrl(ClientCredentials clientCredentials, String blob) {
        try {
            return new URL("http", host, port, productUrl + "/post-blob/" + clientCredentials.id() + "/"
                    + clientCredentials.cookie() + "/" + blob);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
    
    public URL obtainClientInfoUrl() {
        try {
            return new URL("http", host, port, productUrl + "/obtain-client-id");
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
    
}
