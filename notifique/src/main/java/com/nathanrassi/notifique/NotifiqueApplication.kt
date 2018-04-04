package com.nathanrassi.notifique

import android.app.Activity
import android.app.Application
import android.app.Service
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import javax.inject.Inject

class NotifiqueApplication : Application(), HasActivityInjector, HasServiceInjector {
  @Inject internal lateinit var activityInjector: DispatchingAndroidInjector<Activity>
  @Inject internal lateinit var serviceInjector: DispatchingAndroidInjector<Service>

  override fun onCreate() {
    DaggerAppComponent.builder()
        .application(this)
        .build()
        .inject(this)
    super.onCreate()
  }

  override fun activityInjector() = activityInjector

  override fun serviceInjector() = serviceInjector
}
