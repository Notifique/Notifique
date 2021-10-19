package com.nathanrassi.notifique

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class NotifiqueApplication : Application(), HasAndroidInjector {
  @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
  @Inject internal lateinit var crashReporter: CrashReporter

  override fun onCreate() {
    if (BuildConfig.FLAVOR == "internal") {
      // TODO: Android R seems to have issues here.
      /*StrictMode.setThreadPolicy(
        ThreadPolicy.Builder()
          .detectAll()
          .penaltyLog()
          .penaltyDeath()
          .build()
      )*/
      StrictMode.setVmPolicy(
        VmPolicy.Builder()
          .detectAll()
          .penaltyLog()
          .penaltyDeath()
          .build()
      )
    }
    createAppComponent()
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
    super.onCreate()
  }

  override fun androidInjector() = androidInjector
}
