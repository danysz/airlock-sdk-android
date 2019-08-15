package com.weather.airlock.sdk.cache;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONArray;

import java.util.ArrayList;


/**
 * @author Denis Voloshin
 */

public class AndroidAirlockService extends InfraAirlockService {

    private final static String TAG = "airlock.CacheManager";

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void readUserGroupsFromDevice(PersistenceHandler ph, Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);

            if (clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                if (item.getText() != null && item.getText().toString().startsWith(Constants.USER_GROUP_CLICK_BOARD_PREFIX)) {
                    try {

                        String userGroupStr = item.getText().toString().substring(Constants.USER_GROUP_CLICK_BOARD_PREFIX.length(), item.getText().toString().length());
                        JSONArray userGroupsJSONArray = new JSONArray(userGroupStr);

                        ArrayList<String> userGroupsArray = new ArrayList<>();
                        for (int i = 0; i < userGroupsJSONArray.length(); i++) {
                            userGroupsArray.add(userGroupsJSONArray.getString(i));
                        }
                        persistenceHandler.storeDeviceUserGroups(userGroupsArray, streamsService);
                    } catch (Exception e) {
                        Log.w(TAG, AirlockMessages.LOG_FAILED_TO_PARSE_GROUPS_FROM_CLIP_BOARD, e);
                    }
                }
            }
        } catch (Exception e) {
            //do nothing the code is running in the test mode, can't create handler inside thread that has not called Looper.prepare()
        }
    }
}
