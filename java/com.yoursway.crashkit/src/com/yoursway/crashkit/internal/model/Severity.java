package com.yoursway.crashkit.internal.model;

public enum Severity {
    
    NORMAL("normal"),

    MAJOR("major"),

    ;
    
    private final String apiName;
    
    private Severity(String apiName) {
        this.apiName = apiName;
    }
    
    @Override
    public String toString() {
        return apiName;
    }
    
}
