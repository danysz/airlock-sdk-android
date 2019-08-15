package com.weather.demo.tests;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by vladr on 08/08/2016.
 */
public class SampleData {
    final public static Collection<Object[]> getSampleData() { // feature name, enabled/disabled, On/Off
        return Arrays.asList(new Object[][]{
                {"F1", true, true},
                {"F2", true, true},
                {"F3", true, false},
                {"F4", true, true},
                {"F5", true, true},
                {"F6", true, false},
                {"F7", true, true},
                {"NonExistingFeature", false, false}
        });
    }
    final public static Collection<Object[]> getSyncedData() { // feature name, enabled/disabled, On/Off
        return Arrays.asList(new Object[][]{
                {"F1", true, true},
                {"F2", true, true},
                {"F3", true, false},
                {"F4", false, false},
                {"F5", true, true}, //TODO What should be the behaviour when the feature is defined locally, but not remotely
                {"F6", true, false},
                {"F7", true, true},
                {"F21", true, true},
                {"F22", true, true},
                {"F55", true, true},
                {"NonExistingFeature", false, false}
        });
    }


}
