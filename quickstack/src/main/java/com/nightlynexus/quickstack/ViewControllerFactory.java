package com.nightlynexus.quickstack;

import android.content.Context;
import android.os.Parcelable;
import android.view.ViewGroup;

public interface ViewControllerFactory<V extends ViewController> extends Parcelable {
  V createView(Context context, ViewGroup parent);
}
