package com.nathanrassi.notifique;

import android.app.Activity;
import android.app.Application;
import android.app.Service;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;

public final class NotifiqueApplication extends Application

  implements HasActivityInjector, HasServiceInjector {
  @Inject DispatchingAndroidInjector<Activity> activityInjector;
  @Inject DispatchingAndroidInjector<Service> serviceInjector;


  @Override public void onCreate() {
    DaggerAppComponent.builder().application(this).build().inject(this);
    super.onCreate();
  }

  @Override public AndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }

  @Override public AndroidInjector<Service> serviceInjector() {
    return serviceInjector;
  }
}