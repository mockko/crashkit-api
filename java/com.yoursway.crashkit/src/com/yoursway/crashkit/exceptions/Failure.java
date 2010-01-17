package com.yoursway.crashkit.exceptions;

import java.util.HashMap;
import java.util.Map;

public class Failure extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final Map<String, Object> details = new HashMap<String, Object>();
    
    public Failure(String message, Throwable cause) {
        super(message, cause);
    }
    
    public Failure(String message) {
        super(message);
    }
    
    protected Failure() {
        super();
    }
    
    public Failure(Throwable cause) {
        super(cause);
    }
    
    public Failure add(String name, Object value) {
        if (name == null)
            throw new NullPointerException("name is null");
        details.put(name, value);
        return this;
    }
    
    public Map<String, Object> feedbackDetails() {
        return details;
    }
    
}
