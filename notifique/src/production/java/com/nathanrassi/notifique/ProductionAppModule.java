package com.nathanrassi.notifique;

import android.app.Application;
import com.squareup.sqldelight.android.AndroidSqliteDriver;
import com.squareup.sqldelight.db.SqlDriver;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
abstract class ProductionAppModule {
  // Only AppModule should use the Database.
  @AppScope @Provides static Database provideDatabase(Application application) {
    SqlDriver driver =
        new AndroidSqliteDriver(Database.Companion.getSchema(), application, "database.db");
    return Database.Companion.invoke(driver);
  }

  @Binds abstract AppComponent bindsAppComponent(ProductionAppComponent productionAppComponent);
}
