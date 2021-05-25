package com.weather.airlock.sdk;

import android.app.Activity;
import android.content.Context;

import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.BaseAirlockProductManager;
import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.cache.RuntimeLoader;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.inapp.PurchasesManager;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.streams.AirlockStreamResultsTracker;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Base64;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.cache.AndroidContext;
import com.weather.airlock.sdk.cache.AndroidPersistenceHandler;
import com.weather.airlock.sdk.log.AndroidLog;
import com.weather.airlock.sdk.net.AndroidOkHttpClientBuilder;
import com.weather.airlock.sdk.notifications.AndroidNotificationsManager;
import com.weather.airlock.sdk.util.AesGcmEncryptionUtil;
import com.weather.airlock.sdk.util.AndroidBase64;
import com.weather.airlock.sdk.util.FileUtil;
import com.weather.airlytics.AL;
import com.weather.airlytics.environments.ALEnvironment;
import com.weather.airlytics.environments.ALEnvironmentConfig;
import com.weather.airlytics.events.ALEvent;
import com.weather.airlytics.events.ALEventConfig;
import com.weather.airlytics.providers.data.ALProviderConfig;
import com.weather.airlytics.userattributes.ALUserAttribute;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.ibm.airlock.common.util.Constants.SP_AIRLYTICS_EVENT_DEBUG;
import static com.weather.airlock.sdk.AirlyticsConstants.DEBUG_BANNERS_PROVIDER_NAME;


/**
 * The main Airlock class that is used by the application.
 *
 * @author Rachel Levy
 */
@SuppressWarnings("unused")
public class AirlockManager extends BaseAirlockProductManager {

    private static final String ANDROID_PRODUCT_NAME = "ANDROID_PRODUCT_NAME";
    private static final String TAG = "AirlockManager";
    public static final String AIRLYTICS_STREAM_RESULT_EVENT_NAME = "stream-results";
    public static final String AIRLYTICS_STREAM_RESULT_SCHEMA_VERSION = "2.0";
    private static final String DEV_TAG = "DEV";
    private static final String PROD_TAG = "PROD";

    private static final Object lock = new Object();
    private volatile static AirlockManager instance;
    private Context applicationContext = null;

    //airlytics variables
    private static final int DEFAULT_LOCATION_PRECISION = 2;
    private static final int DEFAULT_LOCATION_INTERVAL = 10;
    private final Map<String, ALEnvironment> airlyticsEnvironmentsMap = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> airlyticsPendingEvents = new ConcurrentHashMap<>();
    private static final AtomicBoolean airlyticsEnabled;
    private static final AtomicBoolean airlyticsInitialized;
    private static final AtomicBoolean isDevUser;
    private static final AtomicBoolean disableAirlyticsLifecycle;
    public static String airlyticsUserAttributesSchemaVersion = "17.0";

    private static Set<String> userTags;

    static {
        Base64.init(new AndroidBase64());
        Logger.setLogger(new AndroidLog());
        InMemoryCache.setIsEnabled(false);
        airlyticsInitialized = new AtomicBoolean(false);
        airlyticsEnabled = new AtomicBoolean(false);
        isDevUser = new AtomicBoolean(false);
        disableAirlyticsLifecycle = new AtomicBoolean(true);
        userTags = Collections.synchronizedSet(new HashSet<String>());
    }

    private AirlockManager() {
        super();
    }

    public AirlockManager(String appVersion) {
        super(appVersion);
    }

    /**
     * Returns an AirlockManager instance.
     *
     * @return Returns an AirlockManager instance.
     */
    public static AirlockManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AirlockManager();
                }
            }
        }
        return instance;
    }

    @Override
    public void syncFeatures() throws AirlockNotInitializedException {
        if (!this.init) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        } else {
            cacheManager.syncFeatures();
            updateAirlytics();
        }
    }

    private void updateAirlytics() {
        if (disableAirlyticsLifecycle.get()) {
            return;
        }
        Feature airlyticsFeature =
                getFeature(AirlyticsConstants.AIRLYTICS);
        if (!airlyticsFeature.isOn()) {
            airlyticsEnabled.set(false);
            return;
        }
        airlyticsEnabled.set(true);

        //read and set providers
        Feature providersFeature =
                getFeature(AirlyticsConstants.PROVIDERS);
        List<Feature> providers = providersFeature.getChildren();
        List<ALProviderConfig> providerConfigs = new ArrayList<>();
        for (Feature providerFeature : providers) {
            JSONObject config = providerFeature.getConfiguration();
            if (config != null) {
                ALProviderConfig providerConfig = new ALProviderConfig(config);
                if (providerConfig.getType().equals(DEBUG_BANNERS_PROVIDER_NAME)) {
                    PersistenceHandler persistenceHandler = getCacheManager().getPersistenceHandler();
                    boolean debugEnabled = persistenceHandler.readBoolean(SP_AIRLYTICS_EVENT_DEBUG, providerConfig.getAcceptAllEvents());
                    AL.Companion.setDebugEnable(debugEnabled, DEBUG_BANNERS_PROVIDER_NAME);
                    persistenceHandler.write(SP_AIRLYTICS_EVENT_DEBUG, persistenceHandler.readBoolean(SP_AIRLYTICS_EVENT_DEBUG, providerConfig.getAcceptAllEvents()));
                }
                providerConfigs.add(providerConfig);
            }
        }

        //read and set events
        Feature eventsFeature =
                getFeature(AirlyticsConstants.EVENTS);
        List<Feature> events = eventsFeature.getChildren();
        ArrayList<ALEventConfig> eventConfigs = new ArrayList<>();
        for (Feature eventFeature : events) {
            JSONObject config = eventFeature.getConfiguration();
            if (config != null) {
                eventConfigs.add(new ALEventConfig(config));
            }
        }

        //read and set events
        Feature userAttributesFeature =
                getFeature(AirlyticsConstants.USER_ATTRIBUTES_FEATURE);
        ArrayList<ALUserAttribute> userAttributeConfigs = null;
        if (userAttributesFeature.isOn()){
            List<Feature> userAttributes = userAttributesFeature.getChildren();
            userAttributeConfigs = new ArrayList<>();
            for (Feature userAttribute : userAttributes) {
                JSONObject config = userAttribute.getConfiguration();
                if (config != null) {
                    userAttributeConfigs.add(new ALUserAttribute(config));
                }
            }
        }


        //read and set environments
        Feature environmentFeature =
                getFeature(AirlyticsConstants.ENVIRONMENTS);
        List<Feature> environments = environmentFeature.getChildren();
        if (environments.isEmpty()) {
            airlyticsEnabled.set(false);
        }

        String userId;
        try {
            userId = getAirlockUserUniqueId();
        } catch (AirlockNotInitializedException e) {
            userId = UUID.randomUUID().toString();
            //do nothing - this is called after airlock is initialized already....
        }

        boolean airlyticsJustInitialized = false;

        userTags = getUserTags(null);

        Feature attributeGroupsFeature =
                getFeature(AirlyticsConstants.ATTRIBUTE_GROUPS);
        JSONObject attributeGroupsConfig = attributeGroupsFeature.getConfiguration();
        JSONArray groupsArray = null;
        if (attributeGroupsConfig != null) {
            groupsArray = attributeGroupsConfig.optJSONArray("userAttributesGrouping");
        }

        for (Feature environmentFeatureItem : environments) {
            JSONObject config = environmentFeatureItem.getConfiguration();
            if (config == null) {
                continue;
            }

            ALEnvironmentConfig environmentConfig = new ALEnvironmentConfig(config);
            String environmentName = environmentConfig.getName();
            ALEnvironment environment = null;

            synchronized (airlyticsInitialized) {
                environment = airlyticsEnvironmentsMap.get(environmentName);
                //If this environment requires some user TAG and it does not exist - ignore
                if (Collections.disjoint(environmentConfig.getTags(), userTags)) {
                    //if environment was active and now stopped
                    if (environment != null) {
                        environment.disableEnvironment();
                    }
                    continue;
                }
                if (environment == null) {
                    environmentConfig.setDebugUser(isDevUser.get());
                    if (!airlyticsInitialized.get()) {
                        airlyticsJustInitialized = true;
                        Map<String, String> providersMap = new HashMap<>();
                        providersMap.put(AirlyticsConstants.REST_EVENT_PROXY_NAME, AirlyticsConstants.REST_EVENT_PROXY_HANDLER);
                        providersMap.put(AirlyticsConstants.EVENT_LOG_PROVIDER_NAME, AirlyticsConstants.EVENT_LOG_PROVIDER_HANDLER);
                        providersMap.put(DEBUG_BANNERS_PROVIDER_NAME, AirlyticsConstants.DEBUG_BANNERS_PROVIDER_HANDLER);
                        providersMap.put(AirlyticsConstants.STREAMS_EVENT_PROXY_NAME, AirlyticsConstants.STREAMS_EVENT_PROXY_HANDLER);
                        AL.Companion.registerProviderHandlers(providersMap);
                        if (groupsArray != null) {
                            AL.Companion.setUserAttributeGroups(groupsArray);
                        }
                        airlyticsInitialized.set(true);
                    }
                    environment = AL.Companion.createEnvironment(environmentConfig, providerConfigs, eventConfigs, userAttributeConfigs, userId, UUID.fromString(getProductId()), this.cacheManager.getProductVersion(), applicationContext);
                    airlyticsEnvironmentsMap.put(environmentName, environment);
                } else {
                    boolean enableClientSideValidation = config.optBoolean(AirlyticsConstants.JSON_ENABLE_CLIENTSIDE_VALIDATION);
                    int sessionExpirationInSeconds = config.optInt(AirlyticsConstants.JSON_SESSION_EXPIRATION_IN_SECONDS, 5);
                    environment.update(enableClientSideValidation, sessionExpirationInSeconds, providerConfigs, eventConfigs);
                }
            }

            Map<String, Object> userAttrs = getCalculatedUserAttributes();
            if (!airlyticsEnvironmentsMap.isEmpty()) {
                environment.setUserAttributes(userAttrs, airlyticsUserAttributesSchemaVersion);
            }
        }

        if (airlyticsEnvironmentsMap.isEmpty()) {
            airlyticsEnabled.set(false);
            return;
        }

        if (airlyticsJustInitialized) {
            Feature locationChangedFeature = AirlockManager.getInstance().getFeature(AirlyticsConstants.LOCATION_CHANGED_FEATURE);

            if (locationChangedFeature.isOn()){
                JSONObject configuration = locationChangedFeature.getConfiguration();
                if (configuration != null) {
                    AL.Companion.enableLocationTracking(locationChangedFeature.getConfiguration().optInt("precision", DEFAULT_LOCATION_PRECISION), locationChangedFeature.getConfiguration().optInt("intervalSeconds", DEFAULT_LOCATION_INTERVAL));
                }
            }
            for (String eventName : airlyticsPendingEvents.keySet()) {
                track(eventName, AIRLYTICS_STREAM_RESULT_SCHEMA_VERSION, airlyticsPendingEvents.get(eventName));
            }
            airlyticsPendingEvents.clear();
        }
    }

    private Map<String, Object> getCalculatedUserAttributes() {
        Map<String, Object> userAttributes = new HashMap<>();

        userAttributes.put(AirlyticsConstants.DEV_USER_ATTRIBUTE, isDevUser.get());

        Map<String, String> experimentInfo = getExperimentInfo();
        String variant = null;
        String experiment = null;
        if (experimentInfo != null) {
            variant = experimentInfo.get(Constants.JSON_FIELD_VARIANT);
            experiment = experimentInfo.get(Constants.JSON_FIELD_EXPERIMENT);
            //Do not send to airlytics empty values
            if (variant.isEmpty()) {
                variant = null;
            }
            if (experiment.isEmpty()) {
                experiment = null;
            }
        }
        userAttributes.put(AirlyticsConstants.VARIANT_ATTRIBUTE, variant);
        userAttributes.put(AirlyticsConstants.EXPERIMENT_ATTRIBUTE, experiment);
        return userAttributes;
    }

    private boolean isAirlyticsEnabled() {
        return airlyticsEnabled.get();
    }

    @CheckForNull
    @Override
    public JSONArray addStreamsEvent(JSONObject event) {
        JSONArray events = new JSONArray();
        events.put(event);
        return addStreamsEvent(events, true);
    }

    @CheckForNull
    @Override
    public JSONArray addStreamsEvent(JSONArray events, boolean processImmediately) {
        if (isAirlyticsEnabled() && !getFeature(AirlyticsConstants.USER_ATTRIBUTES_FEATURE).isOn()) {
            return streamsManager.calculateAndSaveStreams(events, processImmediately, null, getContextFieldsForAnalytics(), new AirlockStreamResultsTracker() {
                @Override
                public void trackResults(Map<String, Object> map) {
                    track(AIRLYTICS_STREAM_RESULT_EVENT_NAME, AIRLYTICS_STREAM_RESULT_SCHEMA_VERSION, map);
                }
            });
        } else {
            return super.addStreamsEvent(events, processImmediately);
        }
    }

    public void setAirlyticsUserAttributes(Map<String, Object> attributes, @Nullable String schemaVersion) {
        if (!airlyticsEnabled.get()) {
            return;
        }

        boolean switchedToDev = false;

        if (!isDevUser.get()) {
            Boolean devUser = (Boolean) attributes.get(AirlyticsConstants.DEV_USER_ATTRIBUTE);
            if (devUser != null && devUser) {
                isDevUser.set(true);
                switchedToDev = true;
            }
        }

        if (AirlockManager.userTags.isEmpty()) {
            AirlockManager.userTags = getUserTags(attributes);
        }

        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            if (!Collections.disjoint(environment.getConfig().getTags(), AirlockManager.userTags)) {
                environment.setUserAttributes(attributes, schemaVersion);
            }
        }

        if (switchedToDev) {
            updateAirlytics();
        }
    }

    public void setAirlyticsUserAttribute(String name, @Nullable Object value, @Nullable String schemaVersion) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(name, value);
        setAirlyticsUserAttributes(attributes, schemaVersion);
    }

    public void sendAirlyticsEventsWhenGoingToBackground() {
        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            environment.sendEventsWhenGoingToBackground();
        }
    }

    private Set<String> getUserTags(@Nullable Map<String, Object> attributes) {
        Set<String> userTags = new HashSet<>();
        if (!isDevUser.get()) {
            if (attributes != null) {
                Boolean devUser = (Boolean) attributes.get(AirlyticsConstants.DEV_USER_ATTRIBUTE);
                if (devUser != null) {
                    isDevUser.set(devUser);
                }
            }
            if (!getDeviceUserGroups().isEmpty()
                    || !getDevelopBranchName().isEmpty()) {
                isDevUser.set(true);
            }
        }
        if (isDevUser.get()) {
            userTags.add(DEV_TAG);
        } else {
            userTags.add(PROD_TAG);
        }
        return userTags;
    }

    public void track(String name, @Nullable String schemaVersion, Map<String, Object> attributes) {
        track(name, null, schemaVersion, attributes);
    }

    public void track(String name, @Nullable Long eventTime, @Nullable String schemaVersion, Map<String, Object> attributes) {
        if (!airlyticsEnabled.get()) {
            if (name.equals(AirlyticsConstants.APP_LAUNCH_EVENT) || name.equals(AirlyticsConstants.FIRST_TIME_LAUNCH_EVENT) || name.equals(AirlyticsConstants.NOTIFICATION_INTERACTED_EVENT)) {
                persistAirlyticsAppStart(name, attributes);
            }
            return;
        }
        if (eventTime == null) {
            eventTime = System.currentTimeMillis();
        }
        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            if (!Collections.disjoint(environment.getConfig().getTags(), userTags)) {
                ALEvent event = new ALEvent(name, attributes, eventTime, environment, schemaVersion);
                environment.track(event);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void track(String name, Map<String, Object> attributes) {
        track(name, null, attributes);
    }

    private void persistAirlyticsAppStart(String name, Map<String, Object> attributes) {
        airlyticsPendingEvents.put(name, attributes);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(com.ibm.airlock.common.cache.Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException,
            IOException {
        initSDK(appContext, FileUtil.readAndroidFile(defaultFileId, appContext), productVersion, "");
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @param encryptionKey  Encryption key will be used to encrypt/decrypt the cached data
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void initSDK(com.ibm.airlock.common.cache.Context appContext, int defaultFileId, String productVersion, String encryptionKey) throws
            AirlockInvalidFileException,
            IOException {
        initSDK(appContext, FileUtil.readAndroidFile(defaultFileId, appContext), productVersion, encryptionKey);
    }


    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFile    The defaults file. This defaults file should be part of the application. You can get this by running the Airlock Code Assistant
     *                       plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @SuppressWarnings({"DanglingJavadoc", "WeakerAccess"})
    public synchronized void initSDK(com.ibm.airlock.common.cache.Context appContext, String defaultFile, String productVersion, String encryptionKey, @Nullable Activity mainActivity) throws
            AirlockInvalidFileException,
            IOException {
        /**
         * Allows multiple initSDK calls; skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }
        PersistenceHandler persistenceHandler = new AndroidPersistenceHandler(appContext);
        this.streamsManager = new StreamsManager(persistenceHandler, productVersion);
        this.purchasesManager = new PurchasesManager(persistenceHandler, productVersion);
        this.notificationsManager = new AndroidNotificationsManager(appContext, persistenceHandler, productVersion, getCacheManager().getAirlockContextManager());
        this.cacheManager.init(ANDROID_PRODUCT_NAME, appContext, defaultFile, productVersion, persistenceHandler, this
                .streamsManager, this.notificationsManager, new ConnectionManager(new AndroidOkHttpClientBuilder(), encryptionKey));
        connectionManager = this.cacheManager.getConnectionManager();
        applicationContext = ((AndroidContext) appContext).context;

        if (!isDevUser.get()) {
            List userGroups = getDeviceUserGroups();
            isDevUser.set(userGroups.size() > 0 || !getDevelopBranchName().isEmpty());
        }
        updateAirlytics();
        init = true;
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFile    The defaults file. This defaults file should be part of the application. You can get this by running the Airlock Code Assistant
     *                       plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(com.ibm.airlock.common.cache.Context appContext, String defaultFile, String productVersion, String encryptionKey) throws
            AirlockInvalidFileException,
            IOException {
        initSDK(appContext, defaultFile, productVersion, encryptionKey, null);
    }

    public void enableAirlytics(){
        AirlockManager.disableAirlyticsLifecycle.set(false);
    }


    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext    The current Android context.
     * @param encryptionKey Encryption key will be used to encrypt/decrypt the cached data
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @SuppressWarnings("DanglingJavadoc")
    public synchronized void initSDK(com.ibm.airlock.common.cache.Context appContext, RuntimeLoader runtimeLoader, String encryptionKey) throws
            AirlockInvalidFileException,
            IOException {
        /**
         * Allows multiple initSDK calls; skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }
        PersistenceHandler persistenceHandler = new AndroidPersistenceHandler(appContext);
        this.streamsManager = new StreamsManager(persistenceHandler, appVersion);
        this.notificationsManager = new AndroidNotificationsManager(appContext, persistenceHandler, appVersion, getCacheManager().getAirlockContextManager());
        this.cacheManager.init(ANDROID_PRODUCT_NAME, appContext, "{}", appVersion, persistenceHandler, this
                .streamsManager, this.notificationsManager, new ConnectionManager(new AndroidOkHttpClientBuilder(), encryptionKey));
        connectionManager = this.cacheManager.getConnectionManager();
        runtimeLoader.loadRuntimeFilesOnStartup(this);
        init = true;
    }

    @Override
    public Feature calculateFeatures(@Nullable JSONObject context, String locale) {
        sendContextUserAttributesToAirlytics(context);
        return new Feature();
    }

    @Override
    public void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProducts) throws AirlockNotInitializedException, JSONException {
        super.calculateFeatures(context, purchasedProducts);
        sendContextUserAttributesToAirlytics(context);
    }

    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
        super.calculateFeatures(userProfile, airlockContext);
        sendContextUserAttributesToAirlytics(airlockContext);
    }

    private void sendContextUserAttributesToAirlytics(@Nullable JSONObject airlockContext) {
        if (airlockContext == null) {
            return;
        }
        JSONObject airlyticsContext = airlockContext.optJSONObject(AirlyticsConstants.JSON_AIRLYTICS);
        if (airlockContext != null) {
            Map<String, Object> userAttributes = new HashMap();
            if (airlyticsContext != null) {
                Iterator<?> keys = airlyticsContext.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    Object value = airlyticsContext.opt(key);
                    if (value != null) {
                        userAttributes.put(key, value);
                    }
                }
            }
            if (getFeature(AirlyticsConstants.USER_ATTRIBUTES_FEATURE).isOn()){
                Map analyticsFields = getContextFieldsValuesForAirlyticsAsMap(airlockContext);

                if (!analyticsFields.isEmpty()){
                    userAttributes.putAll(analyticsFields);
                }
                if (!userAttributes.isEmpty()) {
                    setAirlyticsUserAttributes(userAttributes, airlyticsUserAttributesSchemaVersion);
                }
            }
        }
    }

    private void resetRuntime(com.ibm.airlock.common.cache.Context context) {
        try {

            PersistenceHandler sp = cacheManager.getPersistenceHandler();
            if (sp == null) {
                sp = new AndroidPersistenceHandler(context);
                cacheManager.setPersistenceHandler(sp);
            }
            sp.clearRuntimeData();
        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
        }
    }

    @TestOnly
    @Override
    public void reset(com.ibm.airlock.common.cache.Context context, boolean simulateUninstall) {
        try {
            PersistenceHandler sp = cacheManager.getPersistenceHandler();
            if (sp == null) {
                sp = new AndroidPersistenceHandler(context);
                cacheManager.setPersistenceHandler(sp);
            }

            if (simulateUninstall) {
                sp.reset(context);
            } else {
                sp.clearInMemory();
            }
        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
            // continue, this is because the SP is not init
        }
        cacheManager.resetFeatureLists();
        if (streamsManager != null) {
            streamsManager.clearStreams();
        }
        init = false;
    }

    @Override
    public void updateProductContext(String context) {

    }

    @Override
    public void updateProductContext(String context, boolean clearPreviousContext) {

    }


    @Override
    public void removeProductContextField(String fieldPath) {

    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    @TestOnly
    public void reset(Context context) {
        if (cacheManager.getServers() != null) {
            cacheManager.getServers().nullifyServerList();
        }
        this.reset(context, true);
    }


    public void reInitSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException {
        reset(appContext, false);
        initSDK(appContext, defaultFileId, productVersion);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @SuppressWarnings("unused")
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion, boolean isDevUser) throws AirlockInvalidFileException, IOException {
        AirlockManager.isDevUser.set(isDevUser);
        initSDK(new AndroidContext(appContext), defaultFileId, productVersion);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext                The current Android context.
     * @param defaultFileId             Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                                  Code Assistant plugin.
     * @param productVersion            The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @param disableAirlyticsLifeCycle defines if airlytics lifecycle should be disabled
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @SuppressWarnings("unused")
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion, boolean isDevUser, boolean disableAirlyticsLifeCycle, String airlyticsUserAttrVersion) throws AirlockInvalidFileException, IOException {
        AirlockManager.isDevUser.set(isDevUser);
        AirlockManager.disableAirlyticsLifecycle.set(disableAirlyticsLifeCycle);
        AirlockManager.airlyticsUserAttributesSchemaVersion = airlyticsUserAttrVersion;
        initSDK(new AndroidContext(appContext), defaultFileId, productVersion);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException {
        initSDK(new AndroidContext(appContext), defaultFileId, productVersion);
    }


    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion, Activity mainActivity) throws AirlockInvalidFileException, IOException {
        AndroidContext context = new AndroidContext(appContext);
        disableAirlyticsLifecycle.set(false);
        initSDK(context, FileUtil.readAndroidFile(defaultFileId, context), productVersion, "", mainActivity);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @param encryptionKey  Encryption key will be used to encrypt/decrypt the cached data
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion, String encryptionKey) throws AirlockInvalidFileException, IOException {
        initSDK(new AndroidContext(appContext), defaultFileId, productVersion, encryptionKey);
    }


    public Map<String, String> getContextFieldsValuesForAnalyticsAsMap(JSONObject contextObject) {
        JSONObject calculatedFeatures = this.getContextFieldsValuesForAnalytics(contextObject, true);
        Map<String, String> map = new HashMap<>();

        if (calculatedFeatures != null) {
            Iterator<String> keysItr = calculatedFeatures.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                String value = calculatedFeatures.optString(key);
                map.put(key, value);
            }
        }
        return map;
    }

    public Map<String, Object> getContextFieldsValuesForAirlyticsAsMap(JSONObject contextObject) {

        Map<String, Object> map = new HashMap<>();
        JSONObject calculatedFeatures = this.getContextFieldsValuesForAnalytics(contextObject, false);
        if (calculatedFeatures != null) {
            Iterator<String> keysItr = calculatedFeatures.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = calculatedFeatures.opt(key);
                key = key.replace("context.streams.", "streams.");
                map.put(key, value);
            }
        }
        return map;
    }

    public void resetRuntime(Context context) {
        resetRuntime(new AndroidContext(context));
    }

    @TestOnly
    // simulate uninstall.
    public void reset(Context context, boolean simulateUninstall) {
        reset(new AndroidContext(context), simulateUninstall);
    }

    @CheckForNull
    public ALEnvironment getAirlyticsEnvironment(@Nullable String name) {
        if (name == null) {
            return null;
        }
        return AL.Companion.getEnvironment(name);
    }

    @CheckForNull
    public String getSessionDetails(String featureName) {
        String sessionDetails = null;
        JSONObject config = getSessionDetailsFeatureConfig(featureName);
        if (config == null) {
            return null;
        }
        String key = config.optString(Constants.ADS_KEY);
        byte[] details = null;
        int detailsLength = 0;

        JSONArray sessionDetailsFields = config.optJSONArray(Constants.ADS_SESSION_DETAILS_ARRAY);
        if (sessionDetailsFields == null) {
            return null;
        }
        ALEnvironment currentEnvironment = null;

        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            if (!Collections.disjoint(environment.getConfig().getTags(), userTags)) {
                currentEnvironment = environment;
                break;
            }
        }
        if (currentEnvironment == null) {
            return null;
        }

        byte[] detailBytes = getDetailsAsByteArray(sessionDetailsFields, currentEnvironment);

        if (detailBytes == null) {
            return null;
        }

        byte[] cipherText = AesGcmEncryptionUtil.INSTANCE.encrypt(key.getBytes(StandardCharsets.UTF_8), detailBytes, false);

        return com.google.common.io.BaseEncoding.base32().lowerCase().encode(cipherText).replaceAll("=","-");

    }

    private JSONObject getSessionDetailsFeatureConfig(String featureName){
        Feature adSessionDetails = getFeature(featureName);
        if (!adSessionDetails.isOn()) {
            return null;
        }

        return adSessionDetails.getConfiguration();
    }

    public Map<String,String> getSessionDetailsMap(String featureName) {
        Map<String,String> result = new HashMap<>();
        JSONObject config = getSessionDetailsFeatureConfig(featureName);
        if (config == null){
            return result;
        }
        String encodedCipherText = getSessionDetails(featureName);
        if (encodedCipherText == null){
            return result;
        }
        int maxHeaderLength = config.optInt("maxCharactersHeaderLength", 38);
        String headerName = config.optString("headerName","ltv");
        for (int index = 0, ltvIndex = 0; index < encodedCipherText.length();ltvIndex++,index +=maxHeaderLength){
            String headerSuffix = ltvIndex == 0 ? "": String.valueOf(ltvIndex+1);
            int endIndex = Math.min(index+maxHeaderLength, encodedCipherText.length());
            result.put(headerName + headerSuffix, encodedCipherText.substring(index, endIndex));
        }
        return result;
    }

    @CheckForNull
    private byte[] getDetailsAsByteArray(JSONArray sessionDetailsFields, ALEnvironment currentEnvironment) {
        final int BYTE_SIZE = 8;
        int detailsSize = 0;
        Map<String, Object> fieldsMap = new HashMap<>();

        String airlockId;
        try {
            airlockId = getAirlockUserUniqueId();
        } catch (AirlockNotInitializedException e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
            return null;
        }

        //because the order of the fields is important - we can not add the values to byte array immediately
        for (int i = 0; i < sessionDetailsFields.length(); i++) {
            String field = sessionDetailsFields.optString(i);
            switch (field) {
                case Constants.ADS_AIRLOCK_ID:
                    detailsSize += BYTE_SIZE * 2;
                    fieldsMap.put(field, UUID.fromString(airlockId));
                    break;
                case Constants.ADS_SESSION_ID:
                    detailsSize += BYTE_SIZE * 2;
                    fieldsMap.put(field, currentEnvironment.getSessionId());
                    break;
                case Constants.ADS_SESSION_START_TIME:
                    detailsSize += BYTE_SIZE;
                    fieldsMap.put(field, currentEnvironment.getSessionStartTime());
                    break;
            }
        }
        if (detailsSize < 1) {
            return null;
        }
        ByteBuffer detailsByteBuffer = ByteBuffer.wrap(new byte[detailsSize+1]);

        Byte mode = 0;
        if (isDevUser.get()){
            mode = 1;
        }
        detailsByteBuffer.put(mode);
        String[] uuidFields = {Constants.ADS_AIRLOCK_ID, Constants.ADS_SESSION_ID};
        for (String field : uuidFields) {
            UUID uuid = (UUID) fieldsMap.get(field);
            if (uuid != null) {
                detailsByteBuffer.putLong(uuid.getMostSignificantBits());
                detailsByteBuffer.putLong(uuid.getLeastSignificantBits());
            }
        }
        if (fieldsMap.containsKey(Constants.ADS_SESSION_START_TIME)) {
            Long startTime = (Long) fieldsMap.get(Constants.ADS_SESSION_START_TIME);
            if (startTime != null) {
                detailsByteBuffer.putLong(startTime);
            }
        }
        return detailsByteBuffer.array();

    }

    /**
     * Specifies a list of user groups selected for the device.
     *
     * @param userGroups List of the selected user groups.
     */
    @Override
    public void setDeviceUserGroups(@Nullable List<String> userGroups) {
        super.setDeviceUserGroups(userGroups);
        isDevUser.set(true);
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put(AirlyticsConstants.DEV_USER_ATTRIBUTE, true);
        setAirlyticsUserAttributes(userAttributes, airlyticsUserAttributesSchemaVersion);
    }

    @Override
    public String resetAirlockId() {
        String newUUID = super.resetAirlockId();
        AL.Companion.updateUserId(newUUID);
        return newUUID;
    }

    public void verifyAirlyticsState() {
        AL.Companion.verifyLifecycleStarted();
    }

    public Context getApplicationContext() {
        return applicationContext;
    }
}
