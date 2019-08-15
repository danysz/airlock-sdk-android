package com.weather.airlock.sdk.ui;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.services.UserGroupsService;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.R;
import com.weather.airlock.sdk.dagger.AirlockClientsManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * The class contains UI logic which enables a dev user to specify which
 * user groups the specific device has access to.
 * Only features that are associated with these groups will be available on the device.
 *
 * @author Denis Voloshin
 */
public class GroupsManagerActivity extends AppCompatActivity {

    private List<String> filteredUserGroups = new ArrayList();

    private EditText filteredUserGroupView;

    //list of available groups
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    //current user groups with  the  selection choice for the specific device.
    @Nullable
    private Map<String, Boolean> userGroups;

    //current user groups names
    private List<String> userGroupNames;

    @Inject
    UserGroupsService userGroupsService;

    @Inject
    InfraAirlockService infraAirlockService;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AirlockClientsManager.getAirlockClientDiComponent().inject(this);

        //set empty list
        setContentView(R.layout.groups_list);

        //init UI references
        findViewsById();

        //init list
        this.userGroups = new Hashtable<>();

        // disable landscape mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        userGroupsService.getServerUserGroups(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                final String error = String.format(getResources().getString(R.string.retrieving_user_groups), e.getMessage());
                Log.e(Constants.LIB_LOG_TAG, error);
                userGroups = null;
                showToast(error);
            }

            @Override
            public void onSuccess(String userGroups) {
                //parse server response,the response has to be in json format
                try {
                    if (userGroups.isEmpty()) {
                        String warning = getResources().getString(R.string.user_groups_is_empty);
                        Log.w(Constants.LIB_LOG_TAG, warning);
                        showToast(warning);
                        return;
                    }

                    final JSONArray internalUserGroups = new JSONArray(userGroups);
                    GroupsManagerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            GroupsManagerActivity.this.userGroups = generateUserGroupsList(internalUserGroups);

                            Set<String> keys = GroupsManagerActivity.this.userGroups.keySet();
                            GroupsManagerActivity.this.userGroupNames = new ArrayList<String>(keys);

                            Collections.sort(GroupsManagerActivity.this.userGroupNames, new Comparator<String>() {
                                @Override
                                public int compare(String userGroup1, String userGroup2) {
                                    return userGroup1.compareToIgnoreCase(userGroup2);
                                }
                            });

                            int groupsSize = GroupsManagerActivity.this.userGroupNames.size();
                            for (int i = 0; i < groupsSize; i++) {
                                if (GroupsManagerActivity.this.userGroups.get(GroupsManagerActivity.this.userGroupNames.get(i))) {
                                    String selectedGroupName = GroupsManagerActivity.this.userGroupNames.get(i);
                                    GroupsManagerActivity.this.userGroupNames.remove(i);
                                    GroupsManagerActivity.this.userGroupNames.add(0, selectedGroupName);
                                }
                            }

                            filteredUserGroups = GroupsManagerActivity.this.userGroupNames;

                            adapter = new AirlockUserGroupAdapter(GroupsManagerActivity.this,
                                    android.R.layout.simple_list_item_multiple_choice, filteredUserGroups);
                            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                            listView.setAdapter(adapter);

                            for (int i = 0; i < adapter.getCount(); i++) {
                                listView.setItemChecked(i, GroupsManagerActivity.this.userGroups.get(GroupsManagerActivity.this.userGroupNames.get(i)));
                            }

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    // change the user group state

                                    if (view instanceof CheckedTextView) {
                                        CheckedTextView checkedTextView = ((CheckedTextView) view);
                                        Boolean isSelected = checkedTextView.isChecked();
                                        GroupsManagerActivity.this.userGroups.put(GroupsManagerActivity.this.userGroupNames.get(position), isSelected);
                                    }
                                }
                            });
                        }
                    });
                } catch (JSONException e) {
                    final String error = getResources().getString(R.string.user_groups_process_failed);
                    Log.e(Constants.LIB_LOG_TAG, error);
                    showToast(error);
                    Log.e(Constants.LIB_LOG_TAG, "");
                    //render only values from the cache empty list
                }
            }
        });

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );


        filteredUserGroupView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(sequence);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GroupsManagerActivity.this.getBaseContext(), msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        //store new userGroups
        if (userGroups == null) {
            return;
        }
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, Boolean> pair : userGroups.entrySet()) {
            if (pair.getValue()) {
                selected.add(pair.getKey());
            }
        }
        userGroupsService.setDeviceUserGroups(selected);
        writeDataToClipBoard(selected);
        //clear the runtime and try to fetch the develop configuration
        infraAirlockService.clearRuntimeData();

        infraAirlockService.pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(Constants.LIB_LOG_TAG, e.toString());
            }

            @Override
            public void onSuccess(@NonNull String msg) {

            }
        });
    }

    private Map<String, Boolean> generateUserGroupsList(JSONArray serverInternalUserGroups) {
        Map<String, Boolean> userGroups = new Hashtable<>();
        List<String> selectedUserGroups = userGroupsService.getDeviceUserGroups();
        try {
            for (int i = 0; i < serverInternalUserGroups.length(); i++) {
                String groupName = serverInternalUserGroups.getString(i);
                if (selectedUserGroups.contains(groupName)) {
                    userGroups.put(groupName, true);
                } else {
                    userGroups.put(groupName, false);
                }
            }
        } catch (JSONException e) {
            String error = getResources().getString(R.string.user_groups_reading_failed);
            Log.e(Constants.LIB_LOG_TAG, error);
            Toast.makeText(GroupsManagerActivity.this.getBaseContext(), error,
                    Toast.LENGTH_SHORT).show();
        }
        return userGroups;
    }


    private void writeDataToClipBoard(List<String> selectedUserGroups) {
        ClipboardManager clipboard = (ClipboardManager) this
                .getSystemService(GroupsManagerActivity.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(Constants.SP_USER_GROUPS, Constants.USER_GROUP_CLICK_BOARD_PREFIX +
                selectedUserGroups.toString());
        clipboard.setPrimaryClip(clip);
    }

    private void findViewsById() {
        listView = (ListView) findViewById(R.id.list);
        filteredUserGroupView = (EditText) findViewById(R.id.search_bar);
    }

    //*********** Inner Class - AirlockListAdapter *******************//
    public class AirlockUserGroupAdapter extends ArrayAdapter<String> {

        public AirlockUserGroupAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }


        @Override
        public Filter getFilter() {

            Filter filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredUserGroups.clear();
                    filteredUserGroups.addAll((List<String>) results.values);
                    Collections.sort(filteredUserGroups, new Comparator<String>() {
                        @Override
                        public int compare(String userGroup1, String userGroup2) {
                            return userGroup1.compareToIgnoreCase(userGroup2);
                        }
                    });

                    int groupsSize = filteredUserGroups.size();
                    for (int i = 0; i < groupsSize; i++) {
                        if (GroupsManagerActivity.this.userGroups.get(filteredUserGroups.get(i))) {
                            String selectedGroupName = filteredUserGroups.get(i);
                            filteredUserGroups.remove(i);
                            filteredUserGroups.add(0, selectedGroupName);
                        }
                    }
                    notifyDataSetChanged();

                    for (int i = 0; i < adapter.getCount(); i++) {
                        listView.setItemChecked(i, GroupsManagerActivity.this.userGroups.get(GroupsManagerActivity.this.userGroupNames.get(i)));
                    }
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<String> filteredArray = new ArrayList<>();

                    // perform your search here using the searchConstraint String.
                    constraint = constraint.toString().toLowerCase(Locale.getDefault());
                    for (String userGroup : userGroups.keySet()) {
                        if (userGroup.toLowerCase(Locale.getDefault()).contains(constraint)) {
                            filteredArray.add(userGroup);
                        }
                    }
                    results.count = filteredArray.size();
                    results.values = filteredArray;
                    return results;
                }
            };

            return filter;
        }
    }
}
