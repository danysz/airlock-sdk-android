package com.weather.airlock.sdk.dagger;

import com.weather.airlock.sdk.ui.BranchesManagerActivity;
import com.weather.airlock.sdk.ui.DebugExperimentsActivity;
import com.weather.airlock.sdk.ui.DebugFeaturesActivity;
import com.weather.airlock.sdk.ui.EntitlementDetailFragment;
import com.weather.airlock.sdk.ui.EntitlementsListFragment;
import com.weather.airlock.sdk.ui.EventsListFragment;
import com.weather.airlock.sdk.ui.ExperimentDetailsFragment;
import com.weather.airlock.sdk.ui.ExperimentsListFragment;
import com.weather.airlock.sdk.ui.FeatureChildrenListFragment;
import com.weather.airlock.sdk.ui.FeatureDetailsFragment;
import com.weather.airlock.sdk.ui.FeaturesListFragment;
import com.weather.airlock.sdk.ui.GroupsManagerActivity;
import com.weather.airlock.sdk.ui.NotificationDetailFragment;
import com.weather.airlock.sdk.ui.NotificationExtraInfoFragment;
import com.weather.airlock.sdk.ui.NotificationsListFragment;
import com.weather.airlock.sdk.ui.PurchaseOptionDetailFragment;
import com.weather.airlock.sdk.ui.StreamCacheFragment;
import com.weather.airlock.sdk.ui.StreamDetailFragment;
import com.weather.airlock.sdk.ui.StreamResultFragment;
import com.weather.airlock.sdk.ui.StreamsListFragment;
import com.weather.airlock.sdk.ui.StreamsManagerActivity;
import com.weather.airlock.sdk.ui.TraceListFragment;
import com.weather.airlock.sdk.ui.VariantDetailsFragment;

import dagger.Component;

/**
 * @author Denis Voloshin on 2019-07-27.
 */

@Component(modules = {AirlockClientModule.class})
public interface AirlockClientDiComponent {
    void inject(GroupsManagerActivity groupsManagerActivity);
    void inject(BranchesManagerActivity branchesManagerActivity);
    void inject(DebugFeaturesActivity debugFeaturesActivity);
    void inject(ExperimentDetailsFragment experimentDetailsFragment);
    void inject(ExperimentsListFragment experimentsListFragment);
    void inject(FeatureDetailsFragment featureDetailsFragment);
    void inject(VariantDetailsFragment variantDetailsFragment);
    void inject(DebugExperimentsActivity debugExperimentsActivity);
    void inject(FeatureChildrenListFragment featureChildrenListFragment);
    void inject(FeaturesListFragment featuresListFragment);
    void inject(NotificationsListFragment notificationsListFragment);
    void inject(StreamsManagerActivity streamsManagerActivity);
    void inject(EventsListFragment eventsListFragment);
    void inject(StreamCacheFragment streamCacheFragment);
    void inject(StreamDetailFragment streamDetailFragment);
    void inject(StreamResultFragment streamResultFragment);
    void inject(TraceListFragment traceListFragment);
    void inject(StreamsListFragment streamsListFragment);
    void inject(NotificationDetailFragment notificationDetailFragment);
    void inject(NotificationExtraInfoFragment notificationExtraInfoFragment);
    void inject(EntitlementsListFragment entitlementsListFragment);
    void inject(EntitlementDetailFragment entitlementDetailFragment);
    void inject(PurchaseOptionDetailFragment purchaseOptionDetailFragment);
}


