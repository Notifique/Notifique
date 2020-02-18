package com.nathanrassi.notifique

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule

@AppScope
@Component(
    modules = [
      AndroidInjectionModule::class,
      AppModule::class,
      CrashReporterModule::class,
      DebugAppModule::class
    ]
)
internal interface DebugAppComponent : AppComponent {
  fun inject(debugView: DebugView)

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): DebugAppComponent
  }
}

internal fun Application.createAppComponent() = DaggerDebugAppComponent.factory().create(this)
