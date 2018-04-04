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
  @Inject internal lateinit var crashReporter: CrashReporter

  override fun onCreate() {
    super.onCreate()
    DaggerAppComponent.builder()
        .application(this)
        .build()
        .inject(this)

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, e ->
      var cause: Throwable = e
      var forward = cause.cause
      while (forward != null) {
        cause = forward
        forward = forward.cause
      }
      crashReporter.report(cause)
      defaultHandler.uncaughtException(thread, e)
    }
  }

  override fun activityInjector() = activityInjector

  override fun serviceInjector() = serviceInjector
}
