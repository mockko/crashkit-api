package com.yoursway.crashkit;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import com.yoursway.crashkit.internal.CrashKitImpl;
import com.yoursway.crashkit.internal.DisabledCrashKitImpl;
import com.yoursway.crashkit.internal.ServerConnection;
import com.yoursway.crashkit.internal.model.Repository;
import com.yoursway.crashkit.internal.model.Severity;
import com.yoursway.crashkit.internal.utils.YsFileUtils;

public abstract class CrashKit {
    
    private static CrashKit applicationKit;
    
    public static void bug(Throwable throwable) {
        if (applicationKit == null)
            throwable.printStackTrace(System.err);
        else
            applicationKit.report(Severity.NORMAL, throwable);
    }
    
    public static void major(Throwable throwable) {
        if (applicationKit == null)
            throwable.printStackTrace(System.err);
        else
            applicationKit.report(Severity.MAJOR, throwable);
    }
    
    public static CrashKit connectApplication(String userFriendlyProductName,
            String developerFriendlyProductVersion, String feedbackServiceAccountName,
            String feedbackServiceProductName, String[] claimedPackages) {
        String role = determineRole(feedbackServiceAccountName, feedbackServiceProductName);
        final CrashKit kit;
        if ("disabled".equalsIgnoreCase(role))
            kit = new DisabledCrashKitImpl();
        else
            kit = new CrashKitImpl(userFriendlyProductName, developerFriendlyProductVersion, claimedPackages,
                    role, new Repository(userFriendlyProductName), new ServerConnection(
                            feedbackServiceAccountName, feedbackServiceProductName));
        applicationKit = kit;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                major(e);
            }
        });
        return kit;
    }
    
    protected abstract void report(Severity severity, Throwable cause);
    
    private static String determineRole(String accountName, String productName) {
        String unixStyleName = productName.replaceAll("[^a-zA-Z0-9.]+", "");
        String override = System.getProperty(unixStyleName.toLowerCase() + ".feedback.role");
        if (override != null)
            return override;
        override = System.getenv(productName.replaceAll("[^a-zA-Z0-9]+", "_").toUpperCase()
                + "_FEEDBACK_ROLE");
        if (override != null && override.trim().length() > 0)
            return override.trim();
        File path = new File(new File(System.getProperty("user.home")), unixStyleName + ".role");
        try {
            override = YsFileUtils.readAsString(path).trim();
            if (override.length() > 0)
                return override;
        } catch (IOException e) {
        }
        path = new File(new File(System.getProperty("user.home")), "." + unixStyleName + ".role");
        try {
            override = YsFileUtils.readAsString(path).trim();
            if (override.length() > 0)
                return override;
        } catch (IOException e) {
        }
        return "customer";
    }
    
}
