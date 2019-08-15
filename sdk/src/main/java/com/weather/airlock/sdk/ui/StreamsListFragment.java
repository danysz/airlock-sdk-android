package com.weather.airlock.sdk.ui;

/**
 * @author Denis Voloshin on 04/09/2017.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.services.StreamsService;
import com.ibm.airlock.common.streams.AirlockStream;
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


public class StreamsListFragment extends Fragment {

    //list of available streams
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    //current branches list with  the selection choice for this device.
    private Map<String, String> streams;

    //current branch name, by default is 'master'
    private String[] streamNames;

    @Inject
    StreamsService streamsService;

    @Inject
    InfraAirlockService infraAirlockService;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // init Dagger
        AirlockClientsManager.getAirlockClientDiComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.streams_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);

        //init list
        this.streams = new Hashtable<>();
        infraAirlockService.getPersistenceHandler().write(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        infraAirlockService.getPersistenceHandler().write(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
        infraAirlockService.pullStreams(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                final String error = String.format(getResources().getString(R.string.retrieving_streams), e.getMessage());
                Log.e(Constants.LIB_LOG_TAG, error);
                showToast(error);
            }

            @Override
            public void onSuccess(String streamsArrayAsJson) {
                //update streams
                infraAirlockService.getPersistenceHandler().write(Constants.SP_FEATURE_USAGE_STREAMS, streamsArrayAsJson);
                streamsService.updateStreams();

                final JSONArray streamsArray;
                try {
                    streamsArray = new JSONArray(streamsArrayAsJson);
                } catch (JSONException e) {
                    return;
                }

                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        streams = generateStreamsList(streamsArray);
                        java.util.Set<String> keys = streams.keySet();
                        streamNames = (keys.toArray(new String[keys.size()]));
                        Arrays.sort(streamNames);

                        adapter = new ArrayAdapter<String>(getActivity(),
                                android.R.layout.simple_list_item_1, streamNames) {
                            @Override
                            public View getView(int position, @Nullable View convertView, ViewGroup parent) {
                                convertView = new TextView(getContext());
                                convertView.setPadding(20, 30, 20, 30);
                                ((TextView) convertView).setTextSize(15);
                                String streamName = getItem(position);
                                AirlockStream stream = streamsService.getStreamByName(streamName);
                                if (stream != null) {
                                    if (stream.isEnabled() & stream.isAssociatedWithUserGroup()) {
                                        ((TextView) convertView).setTextColor(Color.BLUE);
                                    } else {
                                        ((TextView) convertView).setTextColor(Color.BLACK);
                                    }
                                    ((TextView) convertView).setText(streamName);
                                }
                                return convertView;
                            }
                        };
                        listView.setAdapter(adapter);

                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.streams_list_header, listView, false);
                        listView.addHeaderView(header);

                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                if (position > 0) {
                                    final AirlockStream stream = streamsService.getStreamByName(streamNames[position - 1]);
                                    if (stream == null) {
                                        showToast(getResources().getString(R.string.stream_not_available, streamNames[position - 1]));
                                        return;
                                    }

                                    if (!stream.isEnabled() || !stream.isAssociatedWithUserGroup()) {
                                        StringBuilder sbMessage = new StringBuilder();
                                        if (!stream.isEnabled()) {
                                            sbMessage.append("disabled");
                                            if (!stream.isAssociatedWithUserGroup()) {
                                                sbMessage.append(" and not associated with any active user group");
                                            }
                                        } else {
                                            sbMessage.append("not associated with any active user group");
                                        }
                                        showToast("Stream '" + streamNames[position - 1] + "' is " + sbMessage.toString());
                                        return;
                                    }

                                    FragmentManager manager = getFragmentManager();
                                    FragmentTransaction transaction = manager.beginTransaction();
                                    transaction.add(R.id.container, StreamDetailFragment.newInstance(streamNames[position - 1]), StreamsManagerActivity
                                            .STREAMS_DETAILS_FRAGMENT);
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            }
                        });
                    }
                });
            }
        });

        return view;
    }


    private void showToast(final String msg) {
        Log.d(this.getClass().getName(), msg);
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getBaseContext(), msg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private Map<String, String> generateStreamsList(JSONArray branches) {
        Map<String, String> streamsMap = new Hashtable<>();
        int branchesListLength = branches.length();
        for (int i = 0; i < branchesListLength; i++) {
            JSONObject branchJSON = branches.optJSONObject(i);
            if (branchJSON != null && branchJSON.has("name") && branchJSON.has("uniqueId")) {
                String name = branchJSON.optString("name");
                String uniqueId = branchJSON.optString("uniqueId");
                if (name != null && uniqueId != null) {
                    final AirlockStream stream = streamsService.getStreamByName(name);
                    if (stream != null && stream.isAssociatedWithUserGroup()) {
                        streamsMap.put(name, uniqueId);
                    }
                }
            }
        }
        return streamsMap;
    }
}

