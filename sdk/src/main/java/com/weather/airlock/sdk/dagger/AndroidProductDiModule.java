package com.weather.airlock.sdk.dagger;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.dependency.ProductDiModule;
import com.ibm.airlock.common.engine.context.AirlockContextManager;
import com.ibm.airlock.common.net.BaseOkHttpClientBuilder;
import com.ibm.airlock.common.net.OkHttpConnectionManager;
import com.ibm.airlock.common.services.AnalyticsService;
import com.ibm.airlock.common.services.BranchesService;
import com.ibm.airlock.common.services.ContextService;
import com.ibm.airlock.common.services.EntitlementsService;
import com.ibm.airlock.common.services.FeaturesService;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.services.NotificationService;
import com.ibm.airlock.common.services.PercentageService;
import com.ibm.airlock.common.services.ProductInfoService;
import com.ibm.airlock.common.services.StreamsService;
import com.ibm.airlock.common.services.StringsService;
import com.ibm.airlock.common.services.UserGroupsService;
import com.weather.airlock.sdk.cache.AndroidPersistenceHandler;


public class AndroidProductDiModule extends ProductDiModule {

    public AndroidProductDiModule(Context productContext, String defaultFile,
                                  String productName, String appVersion, String key) {
        super();
        this.productContext = productContext;
        this.productName = productName;
        this.appVersion = appVersion;
        this.key = key;
        this.defaultFile = defaultFile;
        contextManager = new AirlockContextManager(productName);
        infraAirlockService = new InfraAirlockService();
        connectionManager = new OkHttpConnectionManager(new BaseOkHttpClientBuilder(), key);
        persistenceHandler = new AndroidPersistenceHandler(productContext);
        streamsService = new StreamsService();
        userGroupsService = new UserGroupsService();
        branchesService = new BranchesService();
        entitlementsService = new EntitlementsService();
        featuresService = new FeaturesService();
        contextService = new ContextService();
        percentageService = new PercentageService();
        stringsService = new StringsService();
        productInfoService = new ProductInfoService();
        productInfoService = new ProductInfoService();
        analyticsService = new AnalyticsService();
        notificationService = new NotificationService();
    }

}
