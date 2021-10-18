package com.nathanrassi.notifique;

import android.app.Application;
import com.squareup.sqldelight.android.AndroidSqliteDriver;
import com.squareup.sqldelight.db.SqlDriver;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.atomic.AtomicInteger;

@Module
abstract class DebugAppModule {
  @AppScope @Provides static @NotificationIdProvider
  AtomicInteger providesNotificationIdProvider() {
    return new AtomicInteger();
  }

  // Only AppModule should use the Database.
  @AppScope @Provides static Database provideDatabase(
      Application application, DatabaseDelayer databaseDelayer) {
    SqlDriver driver = new DatabaseDelayerSqlDriver(
        new AndroidSqliteDriver(Database.Companion.getSchema(), application, "database.db"),
        databaseDelayer);
    return Database.Companion.invoke(driver);
  }

  @AppScope @Binds abstract AppComponent bindsAppComponent(DebugAppComponent debugAppComponent);
}
