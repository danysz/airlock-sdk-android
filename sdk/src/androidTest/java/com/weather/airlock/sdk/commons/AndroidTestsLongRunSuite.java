package com.weather.airlock.sdk.commons;


import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

/**
 * @author Denis Voloshin
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        //NotificationPercentageRealTest.class,
        //FeaturePercentageRealTest.class,
        //StreamPercentageRealTest.class
})

public class AndroidTestsLongRunSuite {

    @Parameterized.Parameters(name = "Create test helper")
    public static Object[] params() {
        return new Object[][] {{new AndroidSdkBaseTest()}};
    }

    //@Parameterized.Parameter
    //public AbstractBaseTest baseTest ;
}
