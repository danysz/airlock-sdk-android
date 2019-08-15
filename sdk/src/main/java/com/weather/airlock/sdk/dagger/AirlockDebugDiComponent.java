package com.weather.airlock.sdk.dagger;

import com.ibm.airlock.common.dependency.ProductDiModule;

import dagger.Component;

/**
 * @author Denis Voloshin
 */

@Component(modules = ProductDiModule.class)
public interface AirlockDebugDiComponent {
}
