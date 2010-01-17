package com.yoursway.crashkit.exceptions;

import com.yoursway.crashkit.internal.utils.YsDigest;

public class Blob {
    
    private final String body;
    private final String hash;
    
    public Blob(String body) {
        if (body == null)
            throw new NullPointerException("body is null");
        this.body = body;
        this.hash = YsDigest.sha1(body);
    }
    
    public String getBody() {
        return body;
    }
    
    public String getHash() {
        return hash;
    }
    
    @Override
    public String toString() {
        return hash;
    }
    
    public boolean hashEquals(String anotherHash) {
        return hash.equalsIgnoreCase(anotherHash);
    }
    
}
