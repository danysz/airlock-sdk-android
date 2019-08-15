package com.weather.airlock.sdk.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.ibm.airlock.common.model.Entitlement;
import com.ibm.airlock.common.services.EntitlementsService;
import com.weather.airlock.sdk.dagger.AirlockClientsManager;

import java.util.Collection;

import javax.inject.Inject;

/**
 * @author Eitan Schreiber on 29/01/2019.
 */

public class EntitlementsListFragment extends FeaturesListFragment {


    @Inject
    EntitlementsService entitlementsService;

    public EntitlementsListFragment() {
        // Required empty public constructor
    }

    protected void setTitle() {
        getActivity().setTitle("Entitlements");
        searchedTxtView.setHint("Search Entitlements");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // init Dagger
        AirlockClientsManager.getAirlockClientDiComponent().inject(this);
    }

    protected void addAllFeatures() {
        Collection<Entitlement> rootPurchases = entitlementsService.getEntitlements();
        for (Entitlement entitlement : rootPurchases) {
            addEntitlement(entitlement);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    private void addEntitlement(Entitlement entitlement) {
        if (entitlement.getChildren().size() > 0) {
            for (Entitlement entitlementChild : entitlement.getEntitlementChildren()) {
                addEntitlement(entitlementChild);
            }
        }
        if (!originalFeatures.containsKey(entitlement.getName())) {
            this.originalFeatures.put(entitlement.getName(), entitlement);
            this.filteredFeatures.add(entitlement.clone());
        }
    }
}

