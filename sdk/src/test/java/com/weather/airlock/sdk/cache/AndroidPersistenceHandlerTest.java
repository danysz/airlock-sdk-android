package com.weather.airlock.sdk.cache;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.InMemorySharedPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Denis Voloshin
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AndroidPersistenceHandlerTest.class)
public class AndroidPersistenceHandlerTest {

    @Mock
    Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(context.getSharedPreferences(any(String.class), any(Integer.class))).thenReturn(new InMemorySharedPreferences());
        when(context.getInstanceId()).thenReturn("{111-111-111-1111}");
        when(context.getAppVersion()).thenReturn("1.0.0");
        when(context.getAirlockProductName()).thenReturn("TestProductName");
        when(context.getSeasonId()).thenReturn("{222-222-222-222}");
    }

    @Test
    public void init() {
        AndroidPersistenceHandler androidPersistenceHandler = new AndroidPersistenceHandler(context);

    }
    @Test
    public void asyncInit() {
        AndroidPersistenceHandler androidPersistenceHandler = new AndroidPersistenceHandler(context);

    }

    @Test
    public void reset() {
        AndroidPersistenceHandler androidPersistenceHandler = new AndroidPersistenceHandler(context);
        androidPersistenceHandler.write("testKey", "testValue");
        androidPersistenceHandler.reset(context);
        Assert.assertEquals(androidPersistenceHandler.read("testKey", ""), "");
    }

    @Test
    public void write() {
    }

    @Test
    public void writeJSON() {
    }

    @Test
    public void writeStream() {
    }

    @Test
    public void deleteStream() {
    }

    @Test
    public void readStream() {
    }

    @Test
    public void readSinglePreferenceFromFileSystem() {
    }

    @Test
    public void getPurchasesRandomMap() {
    }

    @Test
    public void setPurchasesRandomMap() {
    }


    @Test
    public void reset1() {
    }

    @Test
    public void write2() {
    }

    @Test
    public void write3() {
    }

    @Test
    public void writeStream1() {
    }

    @Test
    public void deleteStream1() {
    }

    @Test
    public void readStream1() {
    }

    @Test
    public void readSinglePreferenceFromFileSystem1() {
    }

    @Test
    public void getPurchasesRandomMap1() {
    }

    @Test
    public void setPurchasesRandomMap1() {
    }
}