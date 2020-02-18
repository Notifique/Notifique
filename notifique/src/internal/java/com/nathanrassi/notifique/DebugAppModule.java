package com.nathanrassi.notifique;

import dagger.Binds;
import dagger.Module;

@Module
abstract class DebugAppModule {
  @Binds abstract AppComponent bindsAppComponent(DebugAppComponent debugAppComponent);
}
