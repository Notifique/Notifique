package com.nathanrassi.notifique

import android.app.Application
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class NotifiqueApplication : Application(), HasAndroidInjector {
  @Inject internal lateinit var androidInjector : DispatchingAndroidInjector<Any>
  @Inject internal lateinit var crashReporter: CrashReporter

  override fun onCreate() {
    super.onCreate()
    DaggerAppComponent.builder()
        .application(this)
        .build()
        .inject(this)

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()!!
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

  override fun androidInjector() = androidInjector
}
