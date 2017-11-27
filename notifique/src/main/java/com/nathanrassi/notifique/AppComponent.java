package com.nathanrassi.notifique;

import android.app.Application;
import android.arch.persistence.room.Room;
import dagger.BindsInstance;
import dagger.Component;
import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import dagger.android.ContributesAndroidInjector;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@AppScope @Component(modules = { AndroidInjectionModule.class, AppComponent.Module.class })
interface AppComponent {
  void inject(NotifiqueApplication app);

  void inject(NotifiqueListView notifiqueListView);

  @Component.Builder interface Builder {
    @BindsInstance AppComponent.Builder application(Application application);

    AppComponent build();
  }

  @dagger.Module abstract class Module {
    @Provides static Notifique.Dao provideNotifiqueDao(@Internal Database database) {
      return database.notifiqueDao();
    }

    @AppScope @Internal @Provides static Database provideDatabase(Application application) {
      return Room.databaseBuilder(application, Database.class, "database").build();
    }

    @Qualifier @Retention(RUNTIME) private @interface Internal {
    }

    @ContributesAndroidInjector
    abstract NotifiqueListenerService contributeNotifiqueListenerService();

    @ContributesAndroidInjector abstract NotifiqueActivity contributeNotifiqueActivity();
  }
}
