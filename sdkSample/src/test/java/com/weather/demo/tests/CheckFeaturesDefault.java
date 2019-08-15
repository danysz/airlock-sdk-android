package com.weather.demo.tests;


import com.weather.demo.BuildConfig;
import com.weather.demo.base.FeatureTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collection;

/**
 * Created by vladr on 08/08/2016.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@Ignore("not in use anymore")
public class CheckFeaturesDefault extends FeatureTest{

    public CheckFeaturesDefault(String featureName, boolean enabled, boolean isOn) {
        super(featureName, enabled, isOn);
    }

    @Before
    public void setup(){
        super.setup();
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "Feature Name = {0}, Feature Enabled = {1}, Feature On = {2}")
    public static Collection<Object[]> data() { // feature name, enabled/disabled, On/Off
        return SampleData.getSampleData();
    }
}
