package com.weather.airlock.sdk.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.R;
import com.weather.airlock.sdk.dagger.AirlockClientsManager;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;


/**
 * @author amirle on 04/09/2017.
 */
public class DebugExperimentsActivity extends AppCompatActivity implements ExperimentsListFragment.OnExperimentSelectedListener, PercentageHolder {

    ExperimentsListFragment listFragment;
    ExperimentDetailsFragment expDetailsFragment;
    VariantDetailsFragment varDetailsFragment;

    JSONObject deviceContext;

    @Inject
    InfraAirlockService infraAirlockService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init Dagger
        AirlockClientsManager.getAirlockClientDiComponent().inject(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.airlock_experiments);

        try {
            deviceContext = new JSONObject(getIntent().getExtras().getString(Constants.DEVICE_CONTEXT));
        } catch (JSONException e) {
            Log.d(this.getClass().getName(), "Failed to fetch device context " + e.getMessage());
        }

        try {
            listFragment = ExperimentsListFragment.newInstance(new JSONObject(infraAirlockService.getPersistenceHandler().read(Constants
                    .JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "")));
        } catch (JSONException e) {
            Log.e(this.getClass().getName(), "Failed to create  ExperimentsListFragment" + e.getMessage());
        }
        FragmentManager fm = getFragmentManager();
        FragmentTransaction tr = fm.beginTransaction();
        tr.replace(R.id.experiments_content_fragment, listFragment);

        tr.commit();
    }

    @Override
    public void onExperimentSelected(JSONObject experiment) {

        expDetailsFragment = ExperimentDetailsFragment.newInstance(experiment);

        getFragmentManager().beginTransaction().replace(R.id.experiments_content_fragment, expDetailsFragment).addToBackStack(null).commit();
    }

    @Override
    public void onVariantSelected(JSONObject variant) {

        varDetailsFragment = VariantDetailsFragment.newInstance(variant);

        getFragmentManager().beginTransaction().replace(R.id.experiments_content_fragment, varDetailsFragment).addToBackStack(null).commit();
    }

    @Override
    public void onPercentageChanged() {
        listFragment.updateListData();
    }

    public JSONObject getDeviceContext() {
        return deviceContext;
    }
}
