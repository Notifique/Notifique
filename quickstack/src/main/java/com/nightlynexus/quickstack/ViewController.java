package com.nightlynexus.quickstack;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public interface ViewController<D extends Parcelable> {
  @NonNull View getView();

  @Nullable D save(); // TODO: Make this a T extends Parcelable.

  // Null when save gives null or on non-restored creation.
  void restore(@Nullable D data);
}
