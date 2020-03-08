package com.nathanrassi.notifique;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class AppModule {
  @Provides static NotifiqueQueries provideNotifqueQueries(Database database) {
    return database.getNotifiqueQueries();
  }

  @ContributesAndroidInjector
  abstract NotifiqueListenerService contributeNotifiqueListenerService();

  @ContributesAndroidInjector abstract NotifiqueActivity contributeNotifiqueActivity();
}
