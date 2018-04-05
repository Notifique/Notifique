package com.nathanrassi.notifique;

import dagger.Module;
import dagger.Provides;

@Module
abstract class CrashReporterModule {
  @AppScope @Provides static CrashReporter providesCrashReporter() {
    return cause -> {
      // No-op.
    };
  }
}
