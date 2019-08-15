package com.weather.airlock.sdk.cache;

import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;


/**
 * @author Denis Voloshin
 */

public class AndroidSharedPreferences implements com.ibm.airlock.common.cache.SharedPreferences {

    private SharedPreferences sharedPreferences;

    public AndroidSharedPreferences(SharedPreferences sp) {
        this.sharedPreferences = sp;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return sharedPreferences.getBoolean(key, defValue);
    }


    @Override
    public long getLong(String key, long defValue) {
        return sharedPreferences.getLong(key, defValue);
    }

    @Override
    public Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return sharedPreferences.getString(key, defValue);
    }

    @Override
    public com.ibm.airlock.common.cache.SharedPreferences.Editor edit() {
        return new AndroidEditor(this.sharedPreferences.edit());
    }

    @Override
    public int getInt(String key, int defValue) {
        return sharedPreferences.getInt(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Object o) {
        return sharedPreferences.getStringSet(key, (Set<String>) o);
    }

    public class AndroidEditor implements com.ibm.airlock.common.cache.SharedPreferences.Editor {
        private SharedPreferences.Editor editor;

        public AndroidEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public Editor remove(String key) {
            return new AndroidEditor(editor.remove(key));
        }

        @Override
        public Editor clear() {
            return new AndroidEditor(editor.clear());
        }

        @Override
        public boolean commit() {
            return editor.commit();
        }

        @Override
        public void apply() {
            editor.apply();
        }

        @Override
        public Editor putInt(String key, int value) {
            return new AndroidEditor(editor.putInt(key, value));
        }

        @Override
        public Editor putLong(String key, long value) {
            return new AndroidEditor(editor.putLong(key, value));
        }

        @Override
        public Editor putFloat(String key, float value) {
            return new AndroidEditor(editor.putFloat(key, value));
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return new AndroidEditor(editor.putBoolean(key, value));
        }

        @Override
        public void putString(String key, String value) {
            editor.putString(key, value);
        }
    }
}
