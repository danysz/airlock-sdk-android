package com.weather.demo.tests;

import com.weather.demo.BuildConfig;
import com.weather.demo.base.FeatureTest;

import org.apache.maven.artifact.ant.shaded.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.fail;

/**
 * Created by vladr on 08/08/2016.
 */
/**
 * @author vladr 04/08/2016.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@Ignore("not in use anymore")
public class ClearCacheOnDevice extends FeatureTest {

    public ClearCacheOnDevice(String featureName, boolean enabled, boolean isOn) {
        super(featureName, enabled, isOn);
    }

    @Before
    public void setup(){
        super.setup();

        try {
            File dir = RuntimeEnvironment.application.getCacheDir();

            if (dir != null && dir.isDirectory()){
                FileUtils.deleteDirectory(dir);
            }
        }
        catch (IOException ex){
            fail("Unable to delete application cache directory");
        }
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "Feature Name = {0}, Feature Enabled = {1}, Feature On = {2}")
    public static Collection<Object[]> data() {
        return SampleData.getSampleData();
    }


}
