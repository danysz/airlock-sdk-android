package com.weather.airlock.sdk;

import android.content.Context;

import com.ibm.airlock.common.AirlockClient;
import com.ibm.airlock.common.DefaultAirlockProductManager;
import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.dependency.DaggerProductDiComponent;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Base64;
import com.weather.airlock.sdk.cache.AndroidContext;
import com.weather.airlock.sdk.cache.AndroidPersistenceHandler;
import com.weather.airlock.sdk.dagger.AndroidProductDiModule;
import com.weather.airlock.sdk.log.AndroidLog;
import com.weather.airlock.sdk.util.AndroidBase64;

import java.io.IOException;


/**
 * Airlock Product manager customized to the Android platform.
 *
 * @author Denis Voloshin
 */
public class AndroidAirlockProductManager extends DefaultAirlockProductManager {

    private static final String ANDROID_PRODUCT_NAME = "ANDROID_PRODUCT_NAME";
    private static final String TAG = "AndroidAirlockProductManager";

    static {
        Base64.init(new AndroidBase64());
        Logger.setLogger(new AndroidLog());
        InMemoryCache.setIsEnabled(false);
    }

    public AndroidAirlockProductManager(String productName, String airlockDefaults, String encryptionKey, String appVersion) {
        super(productName, airlockDefaults, encryptionKey, appVersion);
    }

    public AirlockClient createClient(android.content.Context context, String clientId) throws AirlockInvalidFileException {
        AndroidContext androidContext;
        if (productName != null) {
            androidContext = new AndroidContext(context, productName, appVersion, encryptionKey);
        } else {
            androidContext = new AndroidContext(context, ANDROID_PRODUCT_NAME, appVersion, encryptionKey);
        }

        productDiComponent = DaggerProductDiComponent.builder().productDiModule(
                new AndroidProductDiModule(androidContext, airlockDefaults, androidContext.getAirlockProductName(), appVersion, encryptionKey)).build();
        productDiComponent.inject(this);

        streamsService.init(productDiComponent);
        infraAirlockService.init(productDiComponent);
        notificationService.init(productDiComponent);
        setLocale(persistenceHandler);
        initServices(productDiComponent, infraAirlockService);
        return new AndroidAirlockClient(this, clientId);
    }




    @Override
    public void reset(boolean simulateUninstall) {
        try {
            PersistenceHandler persistenceHandler = infraAirlockService.getPersistenceHandler();
            if (persistenceHandler == null) {
                persistenceHandler = new AndroidPersistenceHandler(context);
                infraAirlockService.setPersistenceHandler(persistenceHandler);
            }

            if (simulateUninstall) {
                persistenceHandler.reset(context);
            } else {
                persistenceHandler.clearInMemory();
            }
        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
            // continue, this is because the SP is not init
        }
        infraAirlockService.resetFeatureLists();
        if (streamsService != null) {
            streamsService.clearStreams();
        }
    }


    public void reset(Context context) {
        if (infraAirlockService.getServers() != null) {
            infraAirlockService.getServers().nullifyServerList();
        }
        this.reset(context);
    }


    /**
     * Initializes AndroidAirlockProductManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion)  {
        // initSDK(new AndroidContext(appContext), defaultFileId, productVersion);
    }

    /**
     * Initializes AndroidAirlockProductManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @param encryptionKey  Encryption key will be used to encrype/decrypt the cached data
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion, String encryptionKey) throws IOException {
        // initSDK(new AndroidContext(appContext), defaultFileId, productVersion, encryptionKey);
    }

//
//    public Map<String, String> getContextFieldsValuesForAnalyticsAsMap(JSONObject contextObject) {
//        JSONObject calculatedFeatures = this.getContextFieldsValuesForAnalytics(contextObject);
//        Map<String, String> map = new HashMap();
//
//        Iterator<String> keysItr = calculatedFeatures.keys();
//        while (keysItr.hasNext()) {
//            String key = keysItr.next();
//            String value = calculatedFeatures.optString(key, "");
//
//            map.put(key, value);
//        }
//        return map;
//    }


//    public void resetRuntime(Context context) {
//        resetRuntime(new AndroidContext(context));
//    }


    public static AndroidProductManagerBuilder builder() {
        return new AndroidProductManagerBuilder();
    }

    public static class AndroidProductManagerBuilder extends AirlockProductManagerBuilder {
        public AndroidAirlockProductManager build() {
            return new AndroidAirlockProductManager(productName, airlockDefaults, encryptionKey, appVersion);
        }
    }
}
