package com.weather.demo.tests;

import com.ibm.airlock.common.AirlockNotInitializedException;
import com.weather.demo.base.FeatureTest;

import org.junit.Before;
import org.junit.Ignore;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Collection;

/**
 * Created by vladr on 10/08/2016.
 */
@Ignore("not in use anymore")
public class CheckBadServerURL extends FeatureTest {


    public CheckBadServerURL(String featureName, boolean enabled, boolean isOn) {
        super(featureName, enabled, isOn);
    }

    @Before
    public void setup(){
        super.setup();

        //manager.setServerURL("http://badURL/1/1");

        //manager.refreshFeatures();
        try {
            manager.syncFeatures();
        } catch (AirlockNotInitializedException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "Feature Name = {0}, Feature Enabled = {1}, Feature On = {2}")
    public static Collection<Object[]> data() { // feature name, enabled/disabled, On/Off
        return SampleData.getSampleData();
    }
}
