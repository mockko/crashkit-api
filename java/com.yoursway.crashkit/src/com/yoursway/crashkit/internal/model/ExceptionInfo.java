package com.yoursway.crashkit.internal.model;

import com.yoursway.jyp.BeanEncoding;

public class ExceptionInfo {
    
    private final String name;
    private final String message;
    private final LocationInfo[] locations;
    
    public ExceptionInfo(@BeanEncoding.Property("name") String name,
            @BeanEncoding.Property("message") String message,
            @BeanEncoding.Property("locations") LocationInfo[] locations) {
        if (name == null)
            throw new NullPointerException("name is null");
        if (locations == null)
            throw new NullPointerException("locations is null");
        this.name = name;
        this.message = message;
        this.locations = locations;
    }
    
    public ExceptionInfo(String name, String message, StackTraceElement[] trace) {
        this(name, message, createLocations(trace));
    }
    
    private static LocationInfo[] createLocations(StackTraceElement[] trace) {
        LocationInfo[] result = new LocationInfo[trace.length];
        for (int i = 0; i < result.length; i++)
            result[i] = new LocationInfo(trace[i]);
        return result;
    }
    
    public String getName() {
        return name;
    }
    
    public String getMessage() {
        if (message == null)
            return "";
        return message;
    }
    
    public LocationInfo[] getLocations() {
        return locations;
    }
    
    public void claimPackages(ClaimedPackages claimedPackages) {
        for (LocationInfo location : locations)
            location.claimPackages(claimedPackages);
    }
    
}
