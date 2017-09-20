package com.nightlynexus.quickstack;

import android.content.Context;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

public final class StateStack<V extends ViewController> {
  private final ViewGroup container;
  private final int index;
  private final ArrayList<StateContainer.SavedState> stack;
  private StateContainer<? extends V> current;

  public StateStack(ViewGroup container, int index, List<StateContainer.SavedState> stack) {
    this.container = container;
    this.index = index;
    this.stack = new ArrayList<>(stack);
  }

  public StateContainer<? extends V> getCurrent() {
    return current;
  }

  public void push(ViewControllerFactory<? extends V> factory, Context dependenciesMap) {
    removeAndPush();
    add(StateContainer.create(factory, dependenciesMap, container));
  }

  public void push(StateContainer.SavedState savedState, Context dependenciesMap) {
    removeAndPush();
    add(StateContainer.<V>createAndRestore(savedState, dependenciesMap, container));
  }

  public void replace(ViewControllerFactory<? extends V> factory, Context dependenciesMap) {
    remove();
    add(StateContainer.create(factory, dependenciesMap, container));
  }

  public void replace(StateContainer.SavedState savedState, Context dependenciesMap) {
    remove();
    add(StateContainer.<V>createAndRestore(savedState, dependenciesMap, container));
  }

  public boolean pop(Context dependenciesMap) {
    if (stack.isEmpty()) {
      return false;
    }
    container.removeViewAt(index);
    StateContainer.SavedState savedState = stack.remove(stack.size() - 1);
    add(StateContainer.<V>createAndRestore(savedState, dependenciesMap, container));
    return true;
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }

  public ArrayList<StateContainer.SavedState> getStack() {
    return new ArrayList<>(stack);
  }

  private void add(StateContainer<? extends V> stateContainer) {
    current = stateContainer;
    container.addView(stateContainer.viewController.getView(), index);
  }

  private void remove() {
    if (current != null) {
      container.removeViewAt(index);
    }
  }

  private void removeAndPush() {
    if (current != null) {
      container.removeViewAt(index);
      stack.add(current.save());
    }
  }
}
