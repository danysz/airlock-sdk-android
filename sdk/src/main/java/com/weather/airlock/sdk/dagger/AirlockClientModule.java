package com.weather.airlock.sdk.dagger;

import com.ibm.airlock.common.AirlockClient;
import com.ibm.airlock.common.services.BranchesService;
import com.ibm.airlock.common.services.EntitlementsService;
import com.ibm.airlock.common.services.FeaturesService;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.services.NotificationService;
import com.ibm.airlock.common.services.StreamsService;
import com.ibm.airlock.common.services.UserGroupsService;

import dagger.Module;
import dagger.Provides;

/**
 * @author Denis Voloshin
 */
@Module
public class AirlockClientModule {

    AirlockClient client;

    public AirlockClientModule(AirlockClient client) {
        this.client = client;
    }

    @Provides
    public InfraAirlockService providesBaseAirlockService() {
        return client.getAirlockProductManager().getInfraAirlockService();
    }

    @Provides
    public StreamsService providesStreamsManager() {
          return client.getAirlockProductManager().getStreamsService();
    }

    @Provides
    public UserGroupsService providesUserGroupsService() {
        return client.getAirlockProductManager().getUserGroupsService();
    }

    @Provides
    public BranchesService providesBranchesService() {
        return client.getAirlockProductManager().getBranchesService();
    }

    @Provides
    public FeaturesService providesFeaturesService() {
        return client.getAirlockProductManager().getFeaturesService();
    }

    @Provides
    public NotificationService providesNotificationService() {
        return client.getAirlockProductManager().getNotificationService();
    }


    @Provides
    public EntitlementsService providesEntitlementsService() {
        return client.getAirlockProductManager().getEntitlementsService();
    }
}
