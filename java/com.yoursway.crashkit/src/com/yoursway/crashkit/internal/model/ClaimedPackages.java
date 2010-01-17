package com.yoursway.crashkit.internal.model;

public class ClaimedPackages {
    
    private final String[] claimedPackages;
    
    public ClaimedPackages(String[] claimedPackages) {
        if (claimedPackages == null)
            throw new NullPointerException("claimedPackages is null");
        this.claimedPackages = claimedPackages;
    }
    
    public boolean isClaimed(String packageName) {
        for (String claimed : claimedPackages)
            if ((packageName + ".").startsWith(claimed + "."))
                return true;
        return false;
    }
    
}
