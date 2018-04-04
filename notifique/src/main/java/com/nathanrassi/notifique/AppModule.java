package com.nathanrassi.notifique;

import android.app.Application;
import android.arch.persistence.room.Room;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Module
abstract class AppModule {
  @Provides static Notifique.Dao provideNotifiqueDao(@Private Database database) {
    return database.notifiqueDao();
  }

  @AppScope @Private @Provides static Database provideDatabase(Application application) {
    return Room.databaseBuilder(application, Database.class, "database").build();
  }

  @Qualifier
  @Retention(RUNTIME)
  private @interface Private {
  }

  @ContributesAndroidInjector
  abstract NotifiqueListenerService contributeNotifiqueListenerService();

  @ContributesAndroidInjector abstract NotifiqueActivity contributeNotifiqueActivity();
}
