package com.nathanrassi.notifique

internal interface CrashReporter {
  fun report(cause: Throwable)
}
