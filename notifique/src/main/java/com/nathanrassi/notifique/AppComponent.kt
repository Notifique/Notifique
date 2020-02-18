package com.nathanrassi.notifique

internal interface AppComponent {
  fun inject(app: NotifiqueApplication)

  fun inject(notifiqueListView: NotifiqueListView)
}
