package com.nathanrassi.notifique;

import dagger.Binds;
import dagger.Module;

@Module
abstract class CrashReporterModule {
  @Binds abstract CrashReporter bindsCrashReporter(DiskCrashReporter diskCrashReporter);
}
