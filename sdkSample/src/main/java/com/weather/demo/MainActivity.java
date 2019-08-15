package com.weather.demo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockClient;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AndroidAirlockProductManager;
import com.weather.airlock.sdk.dagger.AirlockClientModule;
import com.weather.airlock.sdk.dagger.AirlockClientsManager;
import com.weather.airlock.sdk.dagger.DaggerAirlockClientDiComponent;
import com.weather.airlock.sdk.ui.BranchesManagerActivity;
import com.weather.airlock.sdk.ui.DebugExperimentsActivity;
import com.weather.airlock.sdk.ui.DebugFeaturesActivity;
import com.weather.airlock.sdk.ui.EntitlementsManagerActivity;
import com.weather.airlock.sdk.ui.GroupsManagerActivity;
import com.weather.airlock.sdk.ui.NotificationsManagerActivity;
import com.weather.airlock.sdk.ui.StreamsManagerActivity;
import com.weather.airlock.sdk.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private int defaultFileId;
    private AirlockClient androidAirlockClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultFileId = R.raw.airlock_defaults_purchases;
        try {
            AndroidAirlockProductManager.AndroidProductManagerBuilder androidProductManagerBuilder = AndroidAirlockProductManager.builder();

            AndroidAirlockProductManager androidAirlockProductManager1 = (AndroidAirlockProductManager) androidProductManagerBuilder.
                    withAirlockDefaults(FileUtil.readStringFromResource(this, defaultFileId)).
                    withAppVersion("10.0").
                    withConnectionTimeout(10L).
                    withProductName("AirlockTestProd").
                    withSecretKey("").build();

            androidAirlockClient = androidAirlockProductManager1.createClient(getBaseContext(), "111-11-11");

            AirlockClientsManager.
                    setAirlockClientDiComponent(DaggerAirlockClientDiComponent.builder().
                            airlockClientModule(new AirlockClientModule(androidAirlockClient)).build());

            androidAirlockClient.pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Failed to pull: " + e.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onSuccess(String msg) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "pull is Done", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to init exit app ", e);
            finish();
        }
        initActionButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sdk_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_groups:
                startActivity(new Intent(this, GroupsManagerActivity.class));
                break;
            case R.id.menu_branches:
                startActivity(new Intent(this, BranchesManagerActivity.class));
                break;
            case R.id.menu_airlock_experiments:
                Intent intent = new Intent(this, DebugExperimentsActivity.class);
                intent.putExtra(Constants.DEVICE_CONTEXT, "{}");
                startActivity(intent);
                break;
            case R.id.menu_airlock_features:
                Intent featuresActivityIntent = new Intent(this, DebugFeaturesActivity.class);
                featuresActivityIntent.putExtra(Constants.DEVICE_CONTEXT, getDeviceContextAsString());
                featuresActivityIntent.putExtra(Constants.DEFAULT_FILE_ID, defaultFileId);
                featuresActivityIntent.putExtra(Constants.PRODUCT_VERSION, "8.1");
                startActivity(featuresActivityIntent);
                break;
            case R.id.menu_airlock_notifications:
                startActivity(new Intent(this, NotificationsManagerActivity.class));
                break;

            case R.id.menu_init:
                try {
                    // AndroidAirlockProductManager.getInstance().initSDK(getApplicationContext(), defaultFileId, "8.1");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Failed to sync: " + e.toString(), Toast.LENGTH_LONG).show();
                }
                Toast.makeText(getApplicationContext(), "init is Done", Toast.LENGTH_SHORT).show();
                printFeaturesStatus();
                break;

            case R.id.menu_reset:
                androidAirlockClient.getAirlockProductManager().reset(true);
                Toast.makeText(getApplicationContext(), "reset is Done", Toast.LENGTH_SHORT).show();
                printFeaturesStatus();
                break;

            case R.id.streams:
                startActivity(new Intent(this, StreamsManagerActivity.class));
                break;

//            case R.id.menu_data_provider:
//                RemoteConfigurationAsyncFetcher.DataProviderType providerType = AndroidAirlockProductManager.getInstance().getDataProviderType();
//                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setTitle(R.string.data_provider_type_title)
//                        .setSingleChoiceItems(R.array.data_provider_type_array, (providerType.getValue()),
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        AndroidAirlockProductManager.getInstance().setDataProviderType(RemoteConfigurationAsyncFetcher.DataProviderType.getType(which));
//                                    }
//                                });
//                builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                });
//                builder.create().show();
//                break;
//            case R.id.menu_show_sp:
//                SharedPreferences sp = getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
//                String spString = sp.getAll().toString();
//                final AlertDialog.Builder spBuilder = new AlertDialog.Builder(this);
//                spBuilder.setTitle(R.string.shared_preferences_title)
//                        .setMessage(spString);
//                spBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                    }
//                });
//                spBuilder.create().show();
//                break;
//            case R.id.menu_get_last_js_errors:
//
//                final AlertDialog.Builder jsBuilder = new AlertDialog.Builder(this);
//                jsBuilder.setTitle(R.string.last_js_errors_title)
//                        .setMessage(AndroidAirlockProductManager.getInstance().getAirlockService().getLastJSCalculateErrors().toString());
//                jsBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                    }
//                });
//                jsBuilder.create().show();
//                break;
            case R.id.entitlements:
                Intent entitlementsIntent = new Intent(this, EntitlementsManagerActivity.class);
                entitlementsIntent.putExtra(Constants.DEVICE_CONTEXT, getDeviceContextAsString());
                entitlementsIntent.putExtra(Constants.DEFAULT_FILE_ID, defaultFileId);
                entitlementsIntent.putExtra(Constants.PRODUCT_VERSION, "8.1");
                startActivity(entitlementsIntent);
                break;
        }
        return true;
    }


    private void initActionButtons() {
        Button pull = findViewById(R.id.pull_button);
        pull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidAirlockClient.pullFeatures(new AirlockCallback() {
                    @Override
                    public void onFailure(final Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Failed to pull: " + e.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onSuccess(String msg) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Pull is Done", Toast.LENGTH_SHORT).show();
                                printFeaturesStatus();
                            }
                        });
                    }
                });
            }
        });

        Button calculate = findViewById(R.id.calculate_button);
        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //AndroidAirlockProductManager.getInstance().calculate(null, null);
                            AirlockEnginePerformanceMetric metricReporter = AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric();
                            metricReporter.startMeasuring();
                            androidAirlockClient.calculateFeatures(null, new JSONObject(readAndroidFile(R.raw.context_summery)));
                            Log.d("Performance", metricReporter.getReport().toString());
                            metricReporter.stopMeasuring();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Failed to calculate: JSONException", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to calculate:", e);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to calculate:", e);
                        }
                    }
                }).start();
                Toast.makeText(getApplicationContext(), "Calculate is Done", Toast.LENGTH_SHORT).show();
            }
        });

        Button sync = findViewById(R.id.sync_button);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidAirlockClient.syncFeatures();
                Toast.makeText(getApplicationContext(), "Sync is Done", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String readAndroidFile(int fileId) throws AirlockInvalidFileException, IOException {
        if (fileId == Constants.INVALID_FILE_ID) {
            throw new AirlockInvalidFileException(AirlockMessages.ERROR_INVALID_FILE_ID);
        }
        InputStream inStream = this.getResources().openRawResource(fileId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sBuilder = new StringBuilder();
        String strLine;
        while ((strLine = br.readLine()) != null) {
            sBuilder.append(strLine);
        }
        inStream.reset();
        br.close();
        return sBuilder.toString();
    }


    private String getDeviceContextAsString() {
        InputStream ins = getResources().openRawResource(R.raw.big_context);
        BufferedReader br = new BufferedReader(new InputStreamReader(ins));
        StringBuilder sBuilder = new StringBuilder();
        String strLine;
        try {
            while ((strLine = br.readLine()) != null) {
                sBuilder.append(strLine);
            }
            ins.reset();
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "getDeviceContextAsString failed: error, " + e.getMessage());
        }
        return sBuilder.toString();
    }

//    private JSONArray parseChildren(Feature root, FeaturesList fl) {
//        JSONArray result = new JSONArray();
//        List<Feature> children = root.getChildren();
//        for (Feature f : children) {
//            JSONObject childJson = new JSONObject();
//            try {
//                childJson.put(Constants.JSON_FEATURE_FULL_NAME, f.getName());
//                childJson.put(Constants.JSON_FEATURE_IS_ON, f.isOn());
//                childJson.put(Constants.JSON_FEATURE_CONFIGURATION, f.getConfiguration());
//                childJson.put(Constants.JSON_FEATURE_FIELD_FEATURES, parseChildren(f, fl));
//                result.put(childJson);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//        return result;
//    }

    private void printFeaturesStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView features = findViewById(R.id.featureList);
                features.setTextColor(Color.parseColor("#006400"));
                features.setText("");
                String featureList = androidAirlockClient.getAirlockProductManager().getFeaturesService().getSyncFeatureList().printableToString();
                features.setText(featureList);
                Log.d(TAG, "Map = " + featureList);
            }
        });
    }
}
