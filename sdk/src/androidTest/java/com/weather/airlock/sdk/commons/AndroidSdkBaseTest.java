package com.weather.airlock.sdk.commons;

/**
 * @author iditb on 20/11/17.
 */

public class AndroidSdkBaseTest  {


//    public AndroidSdkBaseTest(){
//    }
//
//
//    @Override
//    public void setUpMockups() throws JSONException {
//        manager = AndroidAirlockProductManager.getInstance();
//        MockitoAnnotations.initMocks(this);
//        mockedContext = Mockito.spy(new AndroidContext(InstrumentationRegistry.getContext()));
//
//        doReturn(getDefaultFile()).when(mockedContext).openRawResource(any(Integer.class));
//        Mockito.when(mockedContext.openRawResource(any(Integer.class))).thenReturn(getDefaultFile());
//        mockedContext.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
//    }
//
//
//    @Override
//    public String getDataFileContent(String pathInDataFolder) throws IOException {
//        return (new AndroidSdkTestDataManager()).getFileContent(pathInDataFolder);
//    }
//
//    @Override
//    public String[] getDataFileNames(String directoryPathInDataFolder) throws IOException {
//           return (new AndroidSdkTestDataManager()).getFileNamesListFromDirectory(directoryPathInDataFolder);
//    }
//    @Override
//    protected ConnectionManager getConnectionManager(){
//        return new ConnectionManager(new AndroidOkHttpClientBuilder(true));
//    }
//    @Override
//    protected ConnectionManager getConnectionManager(String m_key){
//        return new ConnectionManager(new AndroidOkHttpClientBuilder(true), m_key);
//    }
//
//    @Override
//    public void setLocale(Locale locale) {
//        Resources resources = InstrumentationRegistry.getTargetContext().getResources();
//        Locale.setDefault(locale);
//        Configuration config = resources.getConfiguration();
//        config.locale = locale;
//        resources.updateConfiguration(config, resources.getDisplayMetrics());
//    }
//
//    @Override
//    public String getTestName() {
//        return null;
//    }
}
