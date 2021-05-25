package com.weather.airlock.sdk.airlytics

import android.content.Context
import com.weather.airlock.sdk.AirlockManager
import com.weather.airlock.sdk.AirlyticsConstants
import com.weather.airlytics.events.ALEvent
import com.weather.airlytics.providers.ALProvider
import com.weather.airlytics.providers.data.ALProviderConfig
import org.json.JSONObject

class StreamsProvider(private var providerConfig: ALProviderConfig) : ALProvider{

    override fun init(context: Context?) {
    }

    private fun convertAlEventToStreamEvent(alEvent: ALEvent):JSONObject{
        return alEvent.toJSONForSend()
    }
    override fun send(event: ALEvent): Boolean {
        if (event.name == AirlockManager.AIRLYTICS_STREAM_RESULT_EVENT_NAME){
            return true
        }
        val alEventToStreamEvent = convertAlEventToStreamEvent(event)
        alEventToStreamEvent.put( AirlyticsConstants.JSON_ANALYTICS_SYSTEM,AirlyticsConstants.SOURCE_ATTRIBUTE_AIRLYTICS)
        AirlockManager.getInstance().addStreamsEvent(alEventToStreamEvent)
        return true
    }

}