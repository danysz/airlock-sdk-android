package com.weather.demo.tests;

import android.app.Activity;

import com.weather.airlock.sdk.AndroidAirlockProductManager;
import com.weather.demo.BuildConfig;
import com.weather.demo.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by vladr on 09/08/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@Ignore("not in use anymore")
public class NegativeTests {

    private AndroidAirlockProductManager manager;
    private int fileThatNotExists = 1234567890;
    private Activity activity;

    @Before
    public void setup(){
        activity = Robolectric.setupActivity(MainActivity.class);
    }

    @After
    public void tearDown(){
        activity.finish();
    }

    @Test
    public void init_sampleFileDoesntExist_shouldFailGracefully(){

        int fileId = fileThatNotExists;

        manager = (AndroidAirlockProductManager)  AndroidAirlockProductManager.getInstance();
        //manager.init(RuntimeEnvironment.application, "123", "Android", fileId);
    }

    @Test
    public void refreshFeatures_brokenSampleFile_shouldFailGracefully(){

        //int fileId = R.raw.sample_broken;
        manager = (AndroidAirlockProductManager)  AndroidAirlockProductManager.getInstance();
        //manager.init(RuntimeEnvironment.application, "123", "Android", fileId);

        //manager.refreshFeatures(true);
        //manager.getFeature("F1");
    }


}