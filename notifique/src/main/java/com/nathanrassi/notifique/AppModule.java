package com.nathanrassi.notifique;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class AppModule {
  @Provides static SavedNotifiqueQueries provideSavedNotifqueQueries(Database database) {
    return database.getSavedNotifiqueQueries();
  }

  @ContributesAndroidInjector
  abstract NotifiqueListenerService contributeNotifiqueListenerService();

  @ContributesAndroidInjector abstract NotifiqueActivity contributeNotifiqueActivity();
}
