package com.weather.airlock.sdk;

import com.ibm.airlock.common.AirlockProductManager;
import com.ibm.airlock.common.DefaultAirlockClient;

/**
 * @author Denis Voloshin
 */
public class AndroidAirlockClient extends DefaultAirlockClient {
    public AndroidAirlockClient(AirlockProductManager airlockProductManager, String id) {
        super(airlockProductManager, id);
    }
}
