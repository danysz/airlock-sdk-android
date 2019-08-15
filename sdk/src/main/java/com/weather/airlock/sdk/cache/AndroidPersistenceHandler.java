package com.weather.airlock.sdk.cache;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.cache.BasePersistenceHandler;
import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.SharedPreferences;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * @author Denis Voloshin
 */

public class AndroidPersistenceHandler extends BasePersistenceHandler {

    private final static String TAG = "AndroidPersistenceHandler";
    protected SharedPreferences instanceSharedPreferences;


    private static final Set<String> instancePreferenceKeys = new HashSet<>(Arrays.asList(
            new String[]{Constants.SP_LAST_CALCULATE_TIME, Constants.SP_LAST_SYNC_TIME
            }
    ));

    private static final Set<String> instanceRuntimeFiles = new HashSet<>(Arrays.asList(
            new String[]{Constants.SP_CURRENT_CONTEXT,
                    Constants.SP_FIRED_NOTIFICATIONS, Constants.SP_NOTIFICATIONS_HISTORY,
                    Constants.SP_SYNCED_FEATURES_LIST,
                    Constants.SP_SERVER_FEATURE_LIST,
                    Constants.SP_PRE_SYNCED_FEATURES_LIST
            }
    ));

    public AndroidPersistenceHandler(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context, AirlockCallback callback) {
        preferences = context.getSharedPreferences(getKey(""), android.content.Context.MODE_PRIVATE);
        instanceSharedPreferences = context.getSharedPreferences(getKey(context.getInstanceId()), android.content.Context.MODE_PRIVATE);
        this.context = context;
        //If first time the app starts or it is a test mock app (files dir is null) - do not read from file system
        if (isInitialized() && context.getFilesDir() != null) {
            new Thread(new AndroidPersistenceHandler.FilePreferencesReader(callback)).start();
        }
    }

    public void init(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        instanceSharedPreferences = context.getSharedPreferences(getKey(context.getInstanceId()), android.content.Context.MODE_PRIVATE);
        //If first time the app starts or it is a test mock app (files dir is null) - do not read from file system
        if (isInitialized() && this.context.getFilesDir() != null) {
            new Thread(new AndroidPersistenceHandler.FilePreferencesReader(null)).start();
        }
    }

    private String getKey(String key) {
        if (instanceRuntimeFiles.contains(key)) {
            return context.getAirlockProductName() + "-" +
                    context.getSeasonId() + "-" + context.getAppVersion() + "-" + context.getInstanceId() + "-" + key;
        } else {
            return context.getAirlockProductName() + "-" + context.getSeasonId() + "-" + context.getAppVersion() + "-" + key;
        }
    }

    public synchronized void reset(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        clear();
    }

    public void write(String key, JSONObject value) {
        if (value != null && value.length() > 0) {
            inMemoryPreferences.put(key, value);
            //if it is a test mock app (files dir is null) - do not write to file system
            if (this.context.getFilesDir() != null) {
                new Thread(new FilePreferencesPreserver(getKey(key), value.toString())).start();
            }
        } else {
            //remove data
            inMemoryPreferences.remove(key);
            context.deleteFile(key);
            new Thread(new FilePreferencesPreserver(getKey(key), value.toString())).start();
        }
    }

    public void write(String key, String value) {
        if (filePersistPreferences.contains(key)) {
            if (value != null && !value.isEmpty()) {
                if (saveAsJSONPreferences.contains(key)) {
                    try {
                        inMemoryPreferences.put(key, new JSONObject(value));
                    } catch (JSONException e) {
                        Logger.log.w(TAG, "Failed to convert content of: " + key + " to JSONObject.");
                    }
                } else {
                    inMemoryPreferences.put(key, value);
                }
                //if it is a test mock app (files dir is null) - do not write to file system
                if (this.context.getFilesDir() != null) {
                    new Thread(new FilePreferencesPreserver(getKey(key), value)).start();
                }
            } else {
                //remove data
                inMemoryPreferences.remove(key);
                context.deleteFile(key);
                if (filePersistPreferences.contains(key)) {
                    new Thread(new FilePreferencesPreserver(getKey(key), value)).start();
                }
            }
        } else {
            if (key.equals(Constants.SP_SEASON_ID)) {
                updateSeasonIdAndClearRuntimeData(value);
                return;
            }
            SharedPreferences.Editor spEditor = preferences.edit();
            if (instancePreferenceKeys.contains(key)) {
                spEditor = instanceSharedPreferences.edit();
            }
            spEditor.putString(key, value);
            spEditor.apply();
        }
    }

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     *
     * @param jsonAsString
     */
    public void writeStream(String name, String jsonAsString) {
        if (jsonAsString != null && !jsonAsString.isEmpty()) {
            //if it is a test mock app (files dir is null) - do not write to file system
            if (this.context.getFilesDir() != null) {
                final long startTime = System.currentTimeMillis();
                try {
                    FileOutputStream fos = context.openFileOutput(getKey(Constants.STREAM_PREFIX + name), android.content.Context.MODE_PRIVATE);
                    if (fos == null) {
                        //On tests that use mock context the FileOutputStream could be null...
                        return;
                    }
                    fos.write(jsonAsString.getBytes());
                    fos.close();
                    Logger.log.d(TAG, "Write to file system of : " + name + " took : " + (System.currentTimeMillis() - startTime));
                } catch (java.io.IOException e) {
                    Logger.log.w(TAG, "Failed to persist content of: " + name + " to file system.");
                }
            }
        } else {
            deleteStream(name);
        }
    }

    public void deleteStream(String name) {
        //if it is a test mock app (files dir is null) - do not write to file system
        if (this.context.getFilesDir() != null) {
            context.deleteFile(getKey(Constants.STREAM_PREFIX + name));
        }
    }

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     */
    public JSONObject readStream(String name) {

        JSONObject value = null;
        String streamValue = (String) readSinglePreferenceFromFileSystem(getKey(Constants.STREAM_PREFIX + name));
        if (streamValue != null) {
            try {
                value = new JSONObject(streamValue);
            } catch (JSONException e) {
                //DO nothing
            }
        }
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }

    @CheckForNull
    @Nullable
    protected Object readSinglePreferenceFromFileSystem(String preferenceName) {
        //because of synchronization it is possible to reach this method but the value is inMemory...
        Object preferenceValue = null;
        synchronized (lock) {
            if (inMemoryPreferences.containsKey(preferenceName)) {
                return inMemoryPreferences.get(preferenceName);
            }
            final long startTime = System.currentTimeMillis();
            FileInputStream fis = null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                fis = context.openFileInput(getKey(preferenceName));
                int fisLength = (int) fis.getChannel().size();
                if (fisLength > 0) {
                    byte[] buffer = new byte[(int) fis.getChannel().size()];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                }
                if (saveAsJSONPreferences.contains(preferenceName)) {
                    preferenceValue = new JSONObject(result.toString("UTF-8"));
                } else {
                    preferenceValue = result.toString("UTF-8");
                }
                inMemoryPreferences.put(preferenceName, preferenceValue);
            } catch (FileNotFoundException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. File not found.");
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to string");
            } catch (JSONException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            } catch (Exception e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            } finally {
                try {
                    fis.close();
                } catch (Throwable ignore) {
                }
            }
            Logger.log.d(TAG, "Read from file system of : " + preferenceName + " took : " + (System.currentTimeMillis() - startTime));
        }
        return preferenceValue;
    }

    public class FilePreferencesPreserver implements Runnable {
        private final String key;
        @Nullable
        private final String value;

        public FilePreferencesPreserver(String key, @Nullable String value) {
            this.key = key;
            this.value = value;
        }

        public void run() {
            final long startTime = System.currentTimeMillis();
            try {
                FileOutputStream fos = context.openFileOutput(getKey(key), android.content.Context.MODE_PRIVATE);
                if (fos == null) {
                    //On tests that use mock context the FileOutputStream could be null...
                    return;
                }
                fos.write(value == null ? "".getBytes() : value.getBytes());
                fos.close();
                Logger.log.d(TAG, "Write to file system of : " + key + " took : " + (System.currentTimeMillis() - startTime));
            } catch (java.io.IOException e) {
                Logger.log.w(TAG, "Failed to persist content of: " + key + " to file system.");
            }
        }
    }

    private class FilePreferencesReader implements Runnable {
        @Nullable
        private AirlockCallback callback;

        public FilePreferencesReader(AirlockCallback callback) {
            this.callback = callback;
        }

        public void run() {
            for (String preferenceName : filePersistPreferences) {
                readSinglePreferenceFromFileSystem(preferenceName);
            }
            if (this.callback != null) {
                this.callback.onSuccess("");
            }
        }
    }

    @CheckForNull
    @Override
    public JSONObject getPurchasesRandomMap() {
        return null;
    }

    @CheckForNull
    @Override
    public void setPurchasesRandomMap(JSONObject randomMap) {

    }
}
