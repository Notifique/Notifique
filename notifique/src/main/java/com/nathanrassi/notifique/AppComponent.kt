package com.nathanrassi.notifique

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule

@AppScope
@Component(modules = [AndroidInjectionModule::class, AppModule::class, CrashReporterModule::class])
internal interface AppComponent {
  fun inject(app: NotifiqueApplication)

  fun inject(notifiqueListView: NotifiqueListView)

  @Component.Builder
  interface Builder {
    @BindsInstance fun application(application: Application): AppComponent.Builder

    fun build(): AppComponent
  }
}
