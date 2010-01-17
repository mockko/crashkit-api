package com.yoursway.crashkit.internal;

import com.yoursway.crashkit.CrashKit;
import com.yoursway.crashkit.exceptions.NullReportedExceptionFailure;
import com.yoursway.crashkit.internal.model.Severity;

public final class DisabledCrashKitImpl extends CrashKit {
    
    protected void report(Severity severity, Throwable cause) {
        if (cause == null)
            cause = new NullReportedExceptionFailure();
        cause.printStackTrace(System.err);
    }
    
}
