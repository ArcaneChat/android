package org.thoughtcrime.securesms.components.viewpager;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

public class RtlSafeViewPager extends HackyViewPager {

  public RtlSafeViewPager(@NonNull Context context) {
    super(context);
  }

  public RtlSafeViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (!isRtl()) {
      return super.onInterceptTouchEvent(ev);
    }

    MotionEvent mirrored = getMirroredMotionEvent(ev);
    try {
      return super.onInterceptTouchEvent(mirrored);
    } finally {
      mirrored.recycle();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (!isRtl()) {
      return super.onTouchEvent(ev);
    }

    MotionEvent mirrored = getMirroredMotionEvent(ev);
    try {
      return super.onTouchEvent(mirrored);
    } finally {
      mirrored.recycle();
    }
  }

  private boolean isRtl() {
    return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
  }

  private MotionEvent getMirroredMotionEvent(@NonNull MotionEvent ev) {
    MotionEvent mirrored = MotionEvent.obtain(ev);
    Matrix matrix = new Matrix();
    matrix.setScale(-1f, 1f, getWidth() / 2f, 0f);
    mirrored.transform(matrix);
    return mirrored;
  }
}
