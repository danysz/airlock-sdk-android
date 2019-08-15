package com.weather.airlock.sdk.commons;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;


/**
 * @author Denis Voloshin on 20/11/17.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
       // StreamsQATest.class
})
public class AndroidTestsSuite {

    @Parameterized.Parameters(name = "Create test helper")
    public static Object[] params() {
        return new Object[][] {{new AndroidSdkBaseTest()}};
    }

    //@Parameterized.Parameter
    //public AbstractBaseTest baseTest ;
}

