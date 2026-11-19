package org.thoughtcrime.securesms.util;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ToolbarScrollColors {
  public static final String SCROLL_TARGET = "toolbar_scroll_target";

  private final View root;
  private final Runnable update;
  private final ViewTreeObserver.OnScrollChangedListener scrollListener;
  private final ViewTreeObserver.OnGlobalLayoutListener layoutListener;

  public ToolbarScrollColors(@NonNull View root, @NonNull Runnable update) {
    this.root = root;
    this.update = update;
    this.scrollListener = update::run;
    this.layoutListener = update::run;
    root.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    root.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
  }

  public void detach() {
    ViewTreeObserver observer = root.getViewTreeObserver();
    if (observer.isAlive()) {
      observer.removeOnScrollChangedListener(scrollListener);
      observer.removeOnGlobalLayoutListener(layoutListener);
    }
  }

  public @Nullable Boolean isScrolled() {
    Rect bounds = new Rect();
    if (!root.getGlobalVisibleRect(bounds)) return null;
    View target = findTarget(root, bounds.centerX());
    return target == null ? null : target.canScrollVertically(-1);
  }

  private @Nullable View findTarget(View view, int centerX) {
    if (!view.isShown()) return null;
    Rect bounds = new Rect();
    if (!view.getGlobalVisibleRect(bounds) || centerX < bounds.left || centerX >= bounds.right) {
      return null;
    }
    if (SCROLL_TARGET.equals(view.getTag())) return view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = group.getChildCount() - 1; i >= 0; i--) {
        View target = findTarget(group.getChildAt(i), centerX);
        if (target != null) return target;
      }
    }
    return null;
  }
}
