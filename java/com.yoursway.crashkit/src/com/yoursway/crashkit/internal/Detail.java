package com.yoursway.crashkit.internal;

import java.util.Map;

import com.yoursway.crashkit.CrashKit;
import com.yoursway.crashkit.exceptions.Failure;

public class Detail {
    
    private final String key;
    private final String value;
    private final Throwable valueCalculationError;
    
    public Detail(String key, Object value) {
        this.key = key;
        String valueAsString;
        Throwable throwable = null;
        try {
            valueAsString = String.valueOf(value);
        } catch (Throwable e) {
            valueAsString = simpleNameOf(value.getClass());
            throwable = e;
        }
        this.value = valueAsString;
        this.valueCalculationError = throwable;
    }
    
    public void addTo(Map<String, String> data) {
        data.put(key, value);
        if (valueCalculationError != null)
            CrashKit.bug(new Failure("Exception in toString() while processing another bug report",
                    valueCalculationError).add("key", key));
    }
    
    private static String simpleNameOf(Class<?> klass) {
        String simpleName = klass.getSimpleName();
        if (simpleName.length() == 0) {
            String fullName = klass.getName();
            simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
        }
        return simpleName;
    }
    
}
