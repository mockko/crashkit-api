package com.yoursway.crashkit.internal.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.yoursway.crashkit.CrashKit;
import com.yoursway.crashkit.exceptions.Blob;
import com.yoursway.crashkit.internal.Constants;
import com.yoursway.crashkit.internal.utils.OS;
import com.yoursway.crashkit.internal.utils.RequestString;
import com.yoursway.crashkit.internal.utils.YsFileUtils;
import com.yoursway.jyp.BeanEncoding;
import com.yoursway.jyp.JSON;
import com.yoursway.jyp.BeanEncoding.BeanificationException;
import com.yoursway.jyp.JSON.SyntaxError;

public class Repository {
    
    private final File settingsFile;
    private final File reportsFolder;
    private final File inProgressReportsFolder;
    private final File attachmentsFolder;
    
    public Repository(String friendlyProductName) {
        if (friendlyProductName == null)
            throw new NullPointerException("friendlyProductName is null");
        File baseFolder = new File(OS.current.applicationDataFolder(friendlyProductName), "Feedback");
        settingsFile = new File(baseFolder, "settings.dat");
        reportsFolder = new File(baseFolder, "Reports");
        attachmentsFolder = new File(baseFolder, "Attachments");
        inProgressReportsFolder = new File(baseFolder, "Reports On The Way");
    }
    
    private void requeueAbandonedFiles() {
        File[] leftoverFiles = inProgressReportsFolder.listFiles();
        if (leftoverFiles == null)
            return;
        long now = System.currentTimeMillis();
        for (File source : leftoverFiles) {
            if (source.lastModified() < now - Constants.REQUEUE_IN_PROGRESS_REPORT_IN) {
                requeue(source);
            }
        }
    }
    
    public ClientCredentials readClientInfo() {
        try {
            Map<String, String> data = RequestString.decode(YsFileUtils.readAsString(settingsFile));
            String clientId = data.get("client_id");
            String clientCookie = data.get("client_cookie");
            if (clientId == null || clientCookie == null)
                return null;
            return new ClientCredentials(clientId, clientCookie);
        } catch (IOException e) {
            return null;
        }
    }
    
    public void writeClientInfo(ClientCredentials info) {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("client_id", info.id());
        data.put("client_cookie", info.cookie());
        settingsFile.getParentFile().mkdirs();
        try {
            YsFileUtils.writeString(settingsFile, RequestString.encode(data));
        } catch (IOException e) {
            // oops, but we can do nothing useful here
        }
    }
    
    public void addReport(Report report, Collection<Blob> attachments) {
        merge(report);
        for (Blob blob : attachments)
            saveAttachment(blob);
    }
    
    private void saveAttachment(Blob blob) {
        File file = new File(attachmentsFolder, blob.getHash());
        try {
            attachmentsFolder.mkdirs();
            if (!file.createNewFile())
                return;
            YsFileUtils.writeString(file, blob.getBody());
        } catch (IOException e) {
            e.printStackTrace(System.err);
            file.delete();
        }
    }
    
    private synchronized void merge(Report obj) {
        File file = new File(reportsFolder, obj.getHash() + ".json");
        try {
            file.getParentFile().mkdirs();
            if (!file.createNewFile()) {
                Report existing = load(file);
                obj.setCount(obj.getCount() + existing.getCount());
            }
        } catch (IOException e) {
        } catch (BeanificationException e) {
        } catch (SyntaxError e) {
        }
        try {
            YsFileUtils.writeString(file, JSON.encode(BeanEncoding.simplify(obj)));
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    
    private Report load(File file) throws BeanificationException, SyntaxError, IOException {
        return BeanEncoding.beanify(JSON.decode(YsFileUtils.readAsString(file)), Report.class);
    }
    
    public synchronized ReportFile obtainReportToSend() {
        requeueAbandonedFiles();
        File[] files = reportsFolder.listFiles();
        if (files == null || files.length == 0)
            return null;
        for (File source : files) {
            inProgressReportsFolder.mkdirs();
            File target = new File(inProgressReportsFolder, source.getName());
            if (target.exists())
                continue;
            source.setLastModified(System.currentTimeMillis());
            source.renameTo(target);
            return new ReportFile(target, this);
        }
        return null;
    }
    
    void requeue(File source) {
        try {
            merge(load(source));
        } catch (IOException e) {
            CrashKit.bug(e);
        } catch (BeanificationException e) {
            CrashKit.bug(e);
        } catch (SyntaxError e) {
            CrashKit.bug(e);
        } catch (Throwable e) {
            CrashKit.bug(e);
        }
        source.delete();
    }
    
    public void deleteClientInfo() {
        settingsFile.delete();
    }
    
    public String obtainBlob(String blobHash) {
        File file = new File(attachmentsFolder, blobHash);
        try {
            Blob blob = new Blob(YsFileUtils.readAsString(file));
            if (!blob.hashEquals(blobHash)) {
                file.delete();
                return null;
            }
            return blob.getBody();
        } catch (IOException e) {
            return null;
        }
    }
    
}
