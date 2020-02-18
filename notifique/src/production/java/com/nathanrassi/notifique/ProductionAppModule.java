package com.nathanrassi.notifique;

import dagger.Binds;
import dagger.Module;

@Module
abstract class ProductionAppModule {
  @Binds abstract AppComponent bindsAppComponent(ProductionAppComponent productionAppComponent);
}
