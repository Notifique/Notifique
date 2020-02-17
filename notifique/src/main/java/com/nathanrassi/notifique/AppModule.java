package com.nathanrassi.notifique;

import android.app.Application;
import com.squareup.sqldelight.android.AndroidSqliteDriver;
import com.squareup.sqldelight.db.SqlDriver;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Module
abstract class AppModule {
  @AppScope @Provides static Database provideDatabase(Application application) {
    SqlDriver driver =
        new AndroidSqliteDriver(Database.Companion.getSchema(), application, "database.db");
    return Database.Companion.invoke(driver);
  }

  @Provides static NotifiqueQueries provideNotifqueQueries(Database database) {
    return database.getNotifiqueQueries();
  }

  @Qualifier
  @Retention(RUNTIME)
  private @interface Private {
  }

  @ContributesAndroidInjector
  abstract NotifiqueListenerService contributeNotifiqueListenerService();

  @ContributesAndroidInjector abstract NotifiqueActivity contributeNotifiqueActivity();
}
