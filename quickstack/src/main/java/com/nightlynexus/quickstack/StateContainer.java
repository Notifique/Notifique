package com.nightlynexus.quickstack;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public final class StateContainer<V extends ViewController<D>, D extends Parcelable> {
  private final ViewControllerFactory<V> factory;
  public final V viewController;

  public static <V extends ViewController<D>, D extends Parcelable> StateContainer<V, D> create(
      ViewControllerFactory<V> factory, Context context, ViewGroup parent) {
    V viewController = factory.createView(context, parent);
    View view = viewController.getView();
    view.setSaveFromParentEnabled(false);
    viewController.restore(null);
    return new StateContainer<>(factory, viewController);
  }

  public static <V extends ViewController<D>, D extends Parcelable> StateContainer<V, D> createAndRestore(
      SavedState savedState, Context context, ViewGroup parent) {
    ViewControllerFactory<V> factory =
        (ViewControllerFactory<V>) savedState.factory; // TODO: Try breaking this.
    V viewController = factory.createView(context, parent);
    View view = viewController.getView();
    view.setSaveFromParentEnabled(false);
    view.restoreHierarchyState(savedState.state);
    viewController.restore(savedState.data);
    return new StateContainer<>(factory, viewController);
  }

  private StateContainer(ViewControllerFactory<V> factory, V viewController) {
    this.factory = factory;
    this.viewController = viewController;
  }

  public SavedState save() {
    SparseArray<Parcelable> state = new SparseArray<>();
    View view = viewController.getView();
    view.saveHierarchyState(state);
    Bundle data = viewController.save();
    return new SavedState(factory, state, data);
  }

  public static final class SavedState implements Parcelable {
    final ViewControllerFactory<?> factory;
    final SparseArray<Parcelable> state;
    @Nullable final Parcelable data;

    SavedState(ViewControllerFactory<?> factory, SparseArray<Parcelable> state,
        @Nullable Parcelable data) {
      this.factory = factory;
      this.state = state;
      this.data = data;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(factory, flags);
      writeSparseArray(dest, state, flags);
      dest.writeParcelable(data, flags);
    }

    @Override public int describeContents() {
      return 0;
    }

    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override public SavedState createFromParcel(Parcel in) {
        ClassLoader classLoader = SavedState.class.getClassLoader();
        ViewControllerFactory factory = in.readParcelable(classLoader);
        SparseArray<Parcelable> state = readSparseArray(in, classLoader);
        Parcelable data = in.readParcelable(classLoader);
        return new SavedState(factory, state, data);
      }

      @Override public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };

    static SparseArray<Parcelable> readSparseArray(Parcel in, ClassLoader loader) {
      int size = in.readInt();
      if (size == -1) {
        return null;
      }
      SparseArray<Parcelable> map = new SparseArray<>(size);
      while (size != 0) {
        int key = in.readInt();
        Parcelable value = in.readParcelable(loader);
        map.append(key, value);
        size--;
      }
      return map;
    }

    static void writeSparseArray(Parcel dest, SparseArray<Parcelable> map, int flags) {
      if (map == null) {
        dest.writeInt(-1);
        return;
      }
      int size = map.size();
      dest.writeInt(size);
      int i = 0;
      while (i != size) {
        dest.writeInt(map.keyAt(i));
        dest.writeParcelable(map.valueAt(i), flags);
        i++;
      }
    }
  }
}
