package com.weather.airlock.sdk.cache;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;


/**
 * @author Denis Voloshin
 */
public class AndroidContext implements Context {

    private android.content.Context context;
    private String airlockProductName;
    private String appVersion;
    private String encryptionKey;
    private String seasonId;
    private String instanceId;

    public AndroidContext(android.content.Context context, String airlockProductName, String appVersion, String encryptionKey) {
        this.context = context;
        this.airlockProductName = airlockProductName;
        this.appVersion = appVersion;
        this.encryptionKey = encryptionKey;
    }

    public void setContext(android.content.Context context) {
        this.context = context;
    }

    @Override
    public String getAirlockProductName() {
        return airlockProductName;
    }

    @Override
    public String getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public String getSeasonId() {
        return null;
    }

    @Override
    public String getInstanceId() {
        return null;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public File getFilesDir() {
        return context.getFilesDir();
    }

    @Override
    public SharedPreferences getSharedPreferences(String spName, int modePrivate) {
        return new AndroidSharedPreferences(this.context.getSharedPreferences(spName, modePrivate));
    }

    @Override
    public void deleteFile(String key) {
        this.context.deleteFile(key);
    }

    @Override
    public FileInputStream openFileInput(String preferenceName) throws FileNotFoundException {
        return this.context.openFileInput(preferenceName);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return this.context.openFileOutput(name, mode);
    }

    @Override
    public Object getSystemService(String name) {
        return this.context.getSystemService(name);
    }

    @Override
    public InputStream openRawResource(int name) {
        return this.context.getResources().openRawResource(name);
    }
}
