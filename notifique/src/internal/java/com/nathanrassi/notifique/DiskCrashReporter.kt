package com.nathanrassi.notifique

import javax.inject.Inject

@AppScope
internal class DiskCrashReporter @Inject constructor() : CrashReporter {
  override fun report(cause: Throwable) {
    // TODO
  }
}
