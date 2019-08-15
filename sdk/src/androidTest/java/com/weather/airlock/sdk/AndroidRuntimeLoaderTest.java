package com.weather.airlock.sdk;

import android.support.test.InstrumentationRegistry;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.cache.AndroidContext;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AndroidRuntimeLoaderTest {

    @Test
    public void readRuntimeFile() {


        AndroidAirlockProductManager manager = AndroidAirlockProductManager.getInstance();
        MockitoAnnotations.initMocks(this);
        Context mockedContext = Mockito.spy(new AndroidContext(InstrumentationRegistry.getContext()));

        mockedContext.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        AndroidRuntimeLoader androidRuntimeLoader = new AndroidRuntimeLoader("runtime","",InstrumentationRegistry.getContext());
        try {
            manager.initSDK(mockedContext, androidRuntimeLoader, "");
        }catch (Exception ex){
            Assert.fail(ex.getMessage());
        }

    }

    @Test
    public void readEncryptedRuntimeFile() {

        AndroidAirlockProductManager manager = AndroidAirlockProductManager.getInstance();
        MockitoAnnotations.initMocks(this);
        Context mockedContext = Mockito.spy(new AndroidContext(InstrumentationRegistry.getContext()));

        mockedContext.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        AndroidRuntimeLoader androidRuntimeLoader = new AndroidRuntimeLoader("runtime/encrypted","TNHI3XTLNXCMDIZ6",InstrumentationRegistry.getContext());
        try{
        manager.initSDK(mockedContext, androidRuntimeLoader, "TNHI3XTLNXCMDIZ6");
        }catch (Exception ex){
            Assert.fail(ex.getMessage());
        }
    }
}