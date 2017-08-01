package com.nathanrassi.notifique;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import javax.inject.Inject;

public final class NotifiqueApplication extends Application
    implements HasActivityInjector, HasServiceInjector {

  @Inject DispatchingAndroidInjector<Activity> dispatchingActivityInjector;
  @Inject DispatchingAndroidInjector<Service> dispatchingServiceInjector;

  @Override public void onCreate() {
    super.onCreate();
    DaggerAppComponent.builder().application(this).build().inject(this);
  }

  @Override public AndroidInjector<Activity> activityInjector() {
    return dispatchingActivityInjector;
  }

  @Override public AndroidInjector<Service> serviceInjector() {
    return dispatchingServiceInjector;
  }
}
