package com.weather.demo.base;

/**
 * Created by vladr on 08/08/2016.
 */

import android.app.Activity;

import com.weather.airlock.sdk.AndroidAirlockProductManager;
import com.weather.demo.BuildConfig;
import com.weather.demo.MainActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Collection;

import static org.junit.Assert.assertSame;

/**
 * @author vladr 04/08/2016.
 */
@Ignore("not in use anymore")
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class FeatureTest {

    protected AndroidAirlockProductManager manager;
    protected String featureName;
    protected boolean featureEnabled;
    protected boolean featureOn;

    public FeatureTest(String featureName, boolean enabled, boolean isOn) {
        this.featureName = featureName;
        this.featureEnabled = enabled;
        this.featureOn = isOn;
    }

    @Before
    public void setup(){
        Activity activity = Robolectric.setupActivity(MainActivity.class);

        //int fileId = R.raw.sample;
        manager = (AndroidAirlockProductManager)  AndroidAirlockProductManager.getInstance();
        //manager.init(RuntimeEnvironment.application, "123", "Android", fileId);
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "Feature Name = {0}, Feature Enabled = {1}, Feature On = {2}")
    public static Collection<Object[]> data(){return null;}

    @Test
    public void isExist_CheckIfEnabled_ShouldBeTheSame(){
        //boolean enabled = manager.getFeature(featureName).isExist();
        //assertSame(featureEnabled, enabled);
    }

    @Test
    public void isOn_CheckIfFeatureOn_ShouldBeTheSame(){
        boolean isOn = manager.getFeature(featureName).isOn();
        assertSame(featureOn, isOn);
    }

}
