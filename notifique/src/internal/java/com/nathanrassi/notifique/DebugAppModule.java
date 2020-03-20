package com.nathanrassi.notifique;

import android.app.Application;
import com.squareup.sqldelight.android.AndroidSqliteDriver;
import com.squareup.sqldelight.db.SqlDriver;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
abstract class DebugAppModule {
  @AppScope @Binds abstract AppComponent bindsAppComponent(DebugAppComponent debugAppComponent);

  // Only AppModule should use the Database.
  @AppScope @Provides static Database provideDatabase(Application application,
      DatabaseDelayer databaseDelayer) {
    SqlDriver driver = new DatabaseDelayerSqlDriver(
        new AndroidSqliteDriver(Database.Companion.getSchema(), application, "database.db"),
        databaseDelayer);
    return Database.Companion.invoke(driver);
  }
}
