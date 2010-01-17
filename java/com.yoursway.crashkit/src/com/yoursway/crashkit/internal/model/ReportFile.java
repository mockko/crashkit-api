package com.yoursway.crashkit.internal.model;

import java.io.File;
import java.io.IOException;

import com.yoursway.crashkit.internal.utils.YsFileUtils;

public class ReportFile {
    
    private final File file;
    private final Repository storage;
    
    public ReportFile(File file, Repository storage) {
        if (file == null)
            throw new NullPointerException("file is null");
        if (storage == null)
            throw new NullPointerException("storage is null");
        this.file = file;
        this.storage = storage;
    }
    
    public String read() throws IOException {
        return YsFileUtils.readAsString(file);
    }
    
    public void delete() {
        file.delete();
    }
    
    public void requeue() {
        storage.requeue(file);
    }
    
}
