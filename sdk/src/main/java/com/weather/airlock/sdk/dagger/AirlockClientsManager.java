package com.weather.airlock.sdk.dagger;

/**
 * @author Denis Voloshin on 2019-07-28.
 */
public class AirlockClientsManager {

    public static AirlockClientDiComponent airlockClientDiComponent;

    public static AirlockClientDiComponent getAirlockClientDiComponent() {
        return airlockClientDiComponent;
    }

    public static void setAirlockClientDiComponent(AirlockClientDiComponent airlockClientDiComponent) {
        AirlockClientsManager.airlockClientDiComponent = airlockClientDiComponent;
    }
}
