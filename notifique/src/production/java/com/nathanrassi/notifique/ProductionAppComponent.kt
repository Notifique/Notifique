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
      ProductionAppModule::class
    ]
)
internal interface ProductionAppComponent : AppComponent {
  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): ProductionAppComponent
  }
}

internal fun Application.createAppComponent() = DaggerProductionAppComponent.factory().create(this)
