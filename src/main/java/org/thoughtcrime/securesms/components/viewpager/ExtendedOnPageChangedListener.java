package org.thoughtcrime.securesms.components.viewpager;

import androidx.viewpager2.widget.ViewPager2;

public abstract class ExtendedOnPageChangedListener extends ViewPager2.OnPageChangeCallback {

  private Integer currentPage = null;

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

  @Override
  public void onPageSelected(int position) {
    if (currentPage != null && currentPage != position) onPageUnselected(currentPage);
    currentPage = position;
  }

  public abstract void onPageUnselected(int position);

  @Override
  public void onPageScrollStateChanged(int state) {}
}
