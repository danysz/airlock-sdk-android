package com.weather.airlock.sdk.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ibm.airlock.common.notifications.AirlockNotification;
import com.ibm.airlock.common.notifications.PendingNotification;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.services.NotificationService;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.R;
import com.weather.airlock.sdk.dagger.AirlockClientsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

import javax.annotation.Nullable;
import javax.inject.Inject;


public class NotificationDetailFragment extends NotificationDataFragment {


    private static String[] DISPLAYS_OPTIONS = {"Status", "Due Date", "Trace", "History", "PreviouslyFired", "Configuration"};
    private static String[] ACTION_OPTIONS = {"Clear History", "Percentage"};

    private ListView action;
    private ListView displayList;
    private BaseAdapter displayAdapter;

    @Inject
    NotificationService notificationService;

    @Inject
    InfraAirlockService infraAirlockService;

    private static final ThreadLocal<DateFormat> yyyyMMddTHHmmss = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        }
    };


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // init Dagger
        AirlockClientsManager.getAirlockClientDiComponent().inject(this);
    }

    public static Fragment newInstance(String notificationName) {
        Fragment fragment = new NotificationDetailFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(NOTIFICATION_NAME, notificationName);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.notification_details, container, false);
        displayList = view.findViewById(R.id.notificationsListDisplay);
        displayAdapter = new CustomDisplay(getFragmentManager(), getActivity(), DISPLAYS_OPTIONS, notificationName);
        displayList.setAdapter(displayAdapter);
        displayList.setItemsCanFocus(true);

        action = view.findViewById(R.id.notificationsListActions);
        action.setAdapter(new CustomAction(getActivity(), ACTION_OPTIONS, notificationName));
        action.setItemsCanFocus(true);

        TextView notification_name = view.findViewById(R.id.notification_name);
        notification_name.setText(this.notificationName);
        return view;
    }


    class CustomDisplay extends BaseAdapter {
        String[] data;
        FragmentManager manager;
        private String notificationName;
        private LayoutInflater inflater;

        public CustomDisplay(FragmentManager manager, Context context, String[] data, String notificationName) {
            this.data = data;
            this.manager = manager;
            this.notificationName = notificationName;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public Object getItem(int position) {
            return data[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (position < 2) {
                view = inflater.inflate(R.layout.display_row_no_action, parent, false);
            } else {
                view = inflater.inflate(R.layout.display_row, parent, false);
            }

            view.setClickable(true);
            view.setFocusable(true);

            if (position == 2) {
                setOnClickListener(view, NotificationExtraInfoFragment.newInstance(notificationName, NotificationExtraInfoFragment.DataType.TRACE), manager);
            }
            if (position == 3) {
                setOnClickListener(view, NotificationExtraInfoFragment.newInstance(notificationName, NotificationExtraInfoFragment.DataType.HISTORY), manager);
            }

            if (position == 4) {
                setOnClickListener(view, NotificationExtraInfoFragment.newInstance(notificationName, NotificationExtraInfoFragment.DataType.PREVIOUSLY_FIRED), manager);
            }

            if (position == 5) {
                setOnClickListener(view, NotificationExtraInfoFragment.newInstance(notificationName, NotificationExtraInfoFragment.DataType.CONFIGURATION), manager);
            }

            TextView title = view.findViewById(R.id.title);
            title.setText(DISPLAYS_OPTIONS[position]);
            TextView value = view.findViewById(R.id.value);

            PendingNotification pendingNotification =
                    notificationService.
                            getPendingNotificationById(notificationService.getNotification(notificationName).getId());

            if (position == 0) {
                if (pendingNotification != null) {
                    value.setText("Scheduled");
                } else {
                    value.setText("UnScheduled");
                }
            }
            if (position == 1) {
                if (pendingNotification != null) {
                    DateFormat df = yyyyMMddTHHmmss.get();
                    value.setText(df.format(Long.valueOf(pendingNotification.getDueDate())));
                } else {
                    value.setText("n/a");
                }
            }
            return view;
        }
    }

    private static void setOnClickListener(View view, final Fragment fragment, final FragmentManager manager) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.container, fragment, NotificationsManagerActivity.NOTIFICATIONS_EVENTS_FRAGMENT);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }


    class CustomAction extends BaseAdapter {
        Context context;
        String[] data;
        private String notificationName;
        private LayoutInflater inflater;

        public CustomAction(Context context, String[] data, String notificationName) {
            this.context = context;
            this.data = data;
            this.notificationName = notificationName;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public Object getItem(int position) {
            return data[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                if (position == 0) {
                    view = inflater.inflate(R.layout.action_row, parent, false);
                } else {
                    view = inflater.inflate(R.layout.action_row_adv, parent, false);
                }
            }
            Button notification_action = view.findViewById(R.id.stream_action);
            view.setClickable(true);
            view.setFocusable(true);

            final AirlockNotification notification = notificationService.getNotification(notificationName);


            if (notification != null) {

                // Clear history
                if (position == 0) {
                    notification_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (notification != null) {
                                //TODO:Clear history action
                            }
                        }
                    });
                }

                // Percentage
                if (position == 1) {
                    ToggleButton notification_checkbox = (ToggleButton) view.findViewById(R.id.checkbox);
                    notification_checkbox.setEnabled(notification.getRolloutPercentage() != 100 && notification.getRolloutPercentage() != 0);
                    try {
                        notification_checkbox.setChecked(Long.valueOf(infraAirlockService.getPersistenceHandler().getNotificationsRandomMap().getString(notification.getName())) <= notification.getRolloutPercentage() * 10000);
                    } catch (JSONException e) {
                        Log.w(Constants.LIB_LOG_TAG, "Error while fetching Notification's random number: ", e);
                    }
                    final TextView stream_action_value = (TextView) view.findViewById(R.id.stream_action_value);
                    notification_checkbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ToggleButton notification_checkbox = (ToggleButton) v;
                            JSONObject notificationsRandomMap = infraAirlockService.getPersistenceHandler().getNotificationsRandomMap();
                            int splitPoint = (int) Math.floor(notification.getRolloutPercentage() * 10000);
                            try {
                                if (notification_checkbox.isChecked()) // select a user random number smaller than the split point
                                {
                                    int rand = new Random().nextInt(splitPoint) + 1;
                                    notificationsRandomMap.put(notification.getName(), String.valueOf(rand));
                                    infraAirlockService.getPersistenceHandler().setNotificationsRandomMap(notificationsRandomMap);

                                } else// select a user random number bigger than the split point
                                {
                                    int rand = new Random().nextInt(1000000 - splitPoint) + splitPoint + 1;
                                    notificationsRandomMap.put(notification.getName(), String.valueOf(rand));
                                    infraAirlockService.getPersistenceHandler().setNotificationsRandomMap(notificationsRandomMap);
                                }
                                notification.setProcessingEnablement();
                            } catch (JSONException e) {
                                Log.w(Constants.LIB_LOG_TAG, "Error while updating Notification's random number: ", e);
                            }

                        }
                    });
                    ((Button) view.findViewById(R.id.stream_action)).setTextColor(Color.BLACK);
                    stream_action_value.setText((notification.getRolloutPercentage()) + "%");
                }
                Button action = view.findViewById(R.id.stream_action);
                action.setText(ACTION_OPTIONS[position]);
            }
            return view;
        }
    }
}

