package org.thoughtcrime.securesms;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import java.lang.reflect.Field;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ResUtil;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ToolbarScrollColors;
import org.thoughtcrime.securesms.util.ViewUtil;

public abstract class BaseActionBarActivity extends AppCompatActivity {

  private static final String TAG = "BaseActionBarActivity";
  protected DynamicTheme dynamicTheme = new DynamicTheme();
  private ToolbarScrollColors toolbarScrollColors;
  private Integer toolbarBackground;
  private boolean actionModeActive;

  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    onPreCreate();
    super.onCreate(savedInstanceState);

    // Only enable Edge-to-Edge if it is well supported
    if (ViewUtil.isEdgeToEdgeSupported()) {
      // docs says to use: WindowCompat.enableEdgeToEdge(getWindow());
      // but it actually makes things worse, the next takes care of setting the 3-buttons navigation
      // bar background
      EdgeToEdge.enable(this);

      WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
          .setAppearanceLightStatusBars(!ThemeUtil.isDarkTheme(this));
    }
  }

  protected Drawable toolbarIcon(@DrawableRes int resource) {
    Drawable icon = DrawableCompat.wrap(ContextCompat.getDrawable(this, resource).mutate());
    DrawableCompat.setTint(icon, ResUtil.getColor(this, R.attr.toolbar_foreground));
    return icon;
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Apply adjustments to the toolbar for edge-to-edge display
    ViewUtil.adjustToolbarForE2E(this);
    if (useToolbarScrollColors()) {
      toolbarScrollColors =
          new ToolbarScrollColors(getWindow().getDecorView(), this::updateToolbarColor);
      updateToolbarColor();
    }
  }

  protected boolean useToolbarScrollColors() {
    return true;
  }

  private void updateToolbarColor() {
    if (toolbarScrollColors == null) return;
    Boolean scrolled = toolbarScrollColors.isScrolled();
    if (scrolled == null && toolbarBackground == null && !actionModeActive) return;
    int color =
        ResUtil.getColor(
            this,
            actionModeActive || Boolean.TRUE.equals(scrolled)
                ? R.attr.colorSurfaceContainer
                : R.attr.toolbar_color);
    if (toolbarBackground != null && toolbarBackground == color) return;
    toolbarBackground = color;

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setElevation(0);
      actionBar.setBackgroundDrawable(new ColorDrawable(color));
    }
    View toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
      toolbar.setBackgroundColor(color);
      if (toolbar.getParent() instanceof AppBarLayout) {
        AppBarLayout appBar = (AppBarLayout) toolbar.getParent();
        appBar.setBackgroundColor(color);
        for (int i = 0; i < appBar.getChildCount(); i++) {
          if (appBar.getChildAt(i) instanceof TabLayout) {
            appBar.getChildAt(i).setBackgroundColor(color);
          }
        }
      }
    }
    View statusBar = findViewById(R.id.status_bar_background);
    if (statusBar != null) statusBar.setBackgroundColor(color);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !ViewUtil.isEdgeToEdgeSupported()) {
      getWindow().setStatusBarColor(color);
    }
  }

  @Override
  public void onSupportActionModeStarted(@NonNull ActionMode mode) {
    super.onSupportActionModeStarted(mode);
    actionModeActive = true;
    updateToolbarColor();
  }

  @Override
  public void onSupportActionModeFinished(@NonNull ActionMode mode) {
    super.onSupportActionModeFinished(mode);
    actionModeActive = false;
    updateToolbarColor();
  }

  @Override
  protected void onDestroy() {
    if (toolbarScrollColors != null) toolbarScrollColors.detach();
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    initializeScreenshotSecurity();
    dynamicTheme.onResume(this);
  }

  private void initializeScreenshotSecurity() {
    if (Prefs.isScreenSecurityEnabled(this)) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
  }

  /** Modified from: http://stackoverflow.com/a/13098824 */
  private void forceOverflowMenu() {
    try {
      ViewConfiguration config = ViewConfiguration.get(this);
      Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
      if (menuKeyField != null) {
        menuKeyField.setAccessible(true);
        menuKeyField.setBoolean(config, false);
      }
    } catch (IllegalAccessException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    } catch (NoSuchFieldException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    }
  }

  public void makeSearchMenuVisible(final Menu menu, final MenuItem searchItem) {
    for (int i = 0; i < menu.size(); ++i) {
      MenuItem item = menu.getItem(i);
      int id = item.getItemId();
      if (id == R.id.menu_search_up || id == R.id.menu_search_down) {
        item.setVisible(true);
      } else if (item != searchItem) {
        item.setVisible(false); // hide all other items
      }
    }
  }

  protected <T extends Fragment> T initFragment(@IdRes int target, @NonNull T fragment) {
    return initFragment(target, fragment, null);
  }

  protected <T extends Fragment> T initFragment(
      @IdRes int target, @NonNull T fragment, @Nullable Bundle extras) {
    Bundle args = new Bundle();

    if (extras != null) {
      args.putAll(extras);
    }

    fragment.setArguments(args);
    getSupportFragmentManager()
        .beginTransaction()
        .replace(target, fragment)
        .commitAllowingStateLoss();
    return fragment;
  }
}
