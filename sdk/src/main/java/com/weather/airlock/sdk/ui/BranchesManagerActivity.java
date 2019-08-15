package com.weather.airlock.sdk.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.services.BranchesService;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.R;
import com.weather.airlock.sdk.dagger.AirlockClientsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;


/**
 * The class contains UI logic which enables a dev user to specify which
 * airlock configuration branch a device will be working with.
 * The branch purpose is to override a master configuration.
 *
 * @author Denis Voloshin
 */
public class BranchesManagerActivity extends AppCompatActivity {

    //list of available branches
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    //current branches list with  the selection choice for this device.
    private Map<String, String> branches;

    //current branch name, by default is 'master'
    private String[] branchNames;


    // holds the name of the selected branch
    @Nullable
    private String selectedDevelopBranch;

    // holds the Id of the selected branch
    @Nullable
    private String selectedDevelopBranchId;

    @Inject
    BranchesService branchesService;

    @Inject
    InfraAirlockService infraAirlockService;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init Dagger
        AirlockClientsManager.getAirlockClientDiComponent().inject(this);
        //set empty list
        setContentView(R.layout.branches_list);

        //init UI references
        findViewsById();

        //init list
        this.branches = new Hashtable<>();

        branchesService.getProductBranches(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                final String error = String.format(getResources().getString(R.string.retrieving_branches), e.getMessage());
                Log.e(Constants.LIB_LOG_TAG, error);
                showToast(error);
            }

            @Override
            public void onSuccess(String branches) {
                try {
                    final JSONArray branchesArray = new JSONArray(branches);
                    BranchesManagerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            BranchesManagerActivity.this.branches = generateBranchesList(branchesArray);
                            java.util.Set<String> keys = BranchesManagerActivity.this.branches.keySet();
                            BranchesManagerActivity.this.branchNames = (keys.toArray(new String[keys.size()]));
                            Arrays.sort(BranchesManagerActivity.this.branchNames);

                            adapter = new ArrayAdapter<>(BranchesManagerActivity.this,
                                    android.R.layout.simple_list_item_single_choice, BranchesManagerActivity.this.branchNames);
                            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                            listView.setAdapter(adapter);

                            for (int i = 1; i <= adapter.getCount(); i++) {
                                if (selectedDevelopBranch != null && selectedDevelopBranch.equals((BranchesManagerActivity.this.branchNames[i - 1]))) {
                                    listView.setItemChecked(i, true);
                                } else {
                                    listView.setItemChecked(i, false);
                                }
                            }

                            LayoutInflater inflater = getLayoutInflater();
                            ViewGroup header = (ViewGroup) inflater.inflate(R.layout.branches_list_header, listView, false);
                            listView.addHeaderView(header);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    AppCompatCheckedTextView checkedView = (AppCompatCheckedTextView) view;
                                    if (checkedView.isChecked()) {
                                        selectedDevelopBranch = BranchesManagerActivity.this.branchNames[position - 1];
                                    }
                                }
                            });
                        }
                    });
                } catch (
                        JSONException e) {
                    final String error = getResources().getString(R.string.user_groups_process_failed);
                    Log.e(Constants.LIB_LOG_TAG, error);
                    showToast(error);
                    Log.e(Constants.LIB_LOG_TAG, "");
                    //render only values from the cache empty list
                }
            }
        });
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BranchesManagerActivity.this.getBaseContext(), msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        final String previousBranchName = branchesService.getDevelopBranchName();
        if (selectedDevelopBranch == null || selectedDevelopBranch.equals("")) {
            branchesService.setDevelopBranch("");
            branchesService.setDevelopBranchName(selectedDevelopBranch);
            branchesService.setDevelopBranchId("");
            return;
        }
        final String selectedDevelopBranchId = branches.get(selectedDevelopBranch);
        branchesService.getProductBranchById(branches.get(selectedDevelopBranch), new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                final String error = String.format(getResources().getString(R.string.retrieving_branch_error), e.getMessage());
                Log.e(Constants.LIB_LOG_TAG, error);
                branchesService.setDevelopBranchName(previousBranchName);
                showToast(error);
            }


            public void onSuccess(String branch)  {

                //parse server response,the response has to be in json format
                try {
                    final JSONObject branchesFullResponse = new JSONObject(branch);
                    branchesService.setDevelopBranch(branchesFullResponse.toString());
                    //apply the selected branch to the master configuration
                    if (selectedDevelopBranch == null) {
                        selectedDevelopBranch = "";
                    }
                    branchesService.setDevelopBranchName(selectedDevelopBranch);
                    branchesService.setDevelopBranchId(selectedDevelopBranchId);
                } catch (JSONException e) {
                    final String error = getResources().getString(R.string.user_groups_process_failed);
                    Log.e(Constants.LIB_LOG_TAG, error);
                    showToast(error);
                    Log.e(Constants.LIB_LOG_TAG, "");
                    branchesService.setDevelopBranchName(previousBranchName);
                }
            }
        });
    }

    private Map<String, String> generateBranchesList(JSONArray branches) {
        Map<String, String> branchesMap = new Hashtable<>();
        selectedDevelopBranch = branchesService.getDevelopBranchName();
        int branchesListLength = branches.length();
        for (int i = 0; i < branchesListLength; i++) {
            JSONObject branchJSON = branches.optJSONObject(i);
            if (branchJSON != null && branchJSON.has("name") && branchJSON.has("uniqueId")) {
                String name = branchJSON.optString("name");
                String uniqueId = branchJSON.optString("uniqueId");
                if (name != null && uniqueId != null) {
                    branchesMap.put(name, uniqueId);
                }
            }
        }
        return branchesMap;
    }

    public void clearBranchSelection(View v) {
        for (int i = 1; i <= adapter.getCount(); i++) {
            listView.setItemChecked(i, false);
        }
        selectedDevelopBranch = "";
    }

    private void findViewsById() {
        listView = (ListView) findViewById(R.id.list);
    }
}
