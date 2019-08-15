package com.weather.airlock.sdk.commons;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.test.InstrumentationRegistry;

import com.weather.airlock.sdk.commons.utils.AndroidTestUtils;

import java.io.IOException;

/**
 * @author iditb on 20/11/17.
 */

public class AndroidSdkTestDataManager  {

    public AndroidSdkTestDataManager(){}


    public String getFileContent(String pathInDataFolder) throws IOException {
        return AndroidTestUtils.readFromAssets(pathInDataFolder);
    }


    public String[] getFileNamesListFromDirectory(String dirPathUnderDataFolder) throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        AssetManager mng = testContext.getAssets();
        String[] list = mng.list(dirPathUnderDataFolder);
        return  list ;
    }

}
