package org.thoughtcrime.securesms.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.ActionBarContextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuItemCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.components.SwitchPreferenceCompat;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ToolbarScrollColors;

@RunWith(AndroidJUnit4.class)
public class MaterialThemeTest {
  private static final int[] THEMES = {
    R.style.TextSecure_LightTheme,
    R.style.TextSecure_LightNoActionBar,
    R.style.TextSecure_DarkTheme,
    R.style.TextSecure_DarkNoActionBar,
    R.style.TextSecure_PurpleTheme,
    R.style.TextSecure_PurpleNoActionBar,
    R.style.TextSecure_PurpleDarkTheme,
    R.style.TextSecure_PurpleDarkNoActionBar,
    R.style.TextSecure_GreenTheme,
    R.style.TextSecure_GreenNoActionBar,
    R.style.TextSecure_GreenDarkTheme,
    R.style.TextSecure_GreenDarkNoActionBar,
    R.style.TextSecure_BlueTheme,
    R.style.TextSecure_BlueNoActionBar,
    R.style.TextSecure_BlueDarkTheme,
    R.style.TextSecure_BlueDarkNoActionBar,
    R.style.TextSecure_RedTheme,
    R.style.TextSecure_RedNoActionBar,
    R.style.TextSecure_RedDarkTheme,
    R.style.TextSecure_RedDarkNoActionBar,
    R.style.TextSecure_PinkTheme,
    R.style.TextSecure_PinkNoActionBar,
    R.style.TextSecure_PinkDarkTheme,
    R.style.TextSecure_PinkDarkNoActionBar,
    R.style.TextSecure_GrayTheme,
    R.style.TextSecure_GrayNoActionBar,
    R.style.TextSecure_GrayDarkTheme,
    R.style.TextSecure_GrayDarkNoActionBar
  };

  @Test
  public void preferenceSwitchUsesMaterial3AndKeepsPreferenceState() {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : THEMES) {
                Context context = themedContext(theme);
                SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
                MaterialSwitch toggle =
                    (MaterialSwitch)
                        LayoutInflater.from(context)
                            .inflate(preference.getWidgetLayoutResource(), null);
                FrameLayout row = new FrameLayout(context);
                row.addView(toggle);
                PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(row);

                preference.setChecked(true);
                preference.onBindViewHolder(holder);
                assertTrue(toggle.isChecked());
                assertEquals(
                    MaterialColors.getColor(context, androidx.appcompat.R.attr.colorAccent, 0),
                    toggle
                        .getTrackTintList()
                        .getColorForState(
                            new int[] {android.R.attr.state_enabled, android.R.attr.state_checked},
                            Color.TRANSPARENT));
                assertTrue(
                    ColorUtils.calculateContrast(
                            toggle
                                .getThumbTintList()
                                .getColorForState(
                                    new int[] {
                                      android.R.attr.state_enabled, android.R.attr.state_checked
                                    },
                                    Color.TRANSPARENT),
                            toggle
                                .getTrackTintList()
                                .getColorForState(
                                    new int[] {
                                      android.R.attr.state_enabled, android.R.attr.state_checked
                                    },
                                    Color.TRANSPARENT))
                        >= 4.5);

                toggle.setChecked(false);
                assertFalse(preference.isChecked());
              }
            });
  }

  @Test
  public void brandedFabsKeepWhiteIcons() {
    int[] layouts = {
      R.layout.conversation_list_fragment,
      R.layout.fragment_search,
      R.layout.activity_relay_list,
      R.layout.all_media_activity
    };
    int[] ids = {R.id.fab, R.id.fab, R.id.fab_add_relay, R.id.fab_add_app};
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : THEMES) {
                Context context = themedContext(theme);
                LayoutInflater inflater = LayoutInflater.from(context);
                for (int i = 0; i < layouts.length; i++) {
                  FloatingActionButton fab = inflater.inflate(layouts[i], null).findViewById(ids[i]);
                  ColorStateList tint = fab.getImageTintList();
                  assertEquals(Color.WHITE, tint.getDefaultColor());
                  assertEquals(
                      MaterialColors.getColor(context, R.attr.fab_color, 0),
                      fab.getBackgroundTintList().getDefaultColor());
                }
              }
            });
  }

  @Test
  public void alertDialogRetainsSelectedThemeColors() {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : THEMES) {
                Context context = themedContext(theme);
                Context dialogContext = new AlertDialog.Builder(context).getContext();
                assertEquals(
                    MaterialColors.getColor(context, R.attr.colorPrimary, 0),
                    MaterialColors.getColor(dialogContext, R.attr.colorPrimary, 0));
                assertEquals(
                    MaterialColors.getColor(context, androidx.appcompat.R.attr.colorAccent, 0),
                    MaterialColors.getColor(
                        dialogContext, androidx.appcompat.R.attr.colorAccent, 0));
                assertEquals(
                    MaterialColors.getColor(context, R.attr.colorSurface, 0),
                    MaterialColors.getColor(dialogContext, R.attr.colorSurface, 0));
              }
            });
  }

  @Test
  public void screenAppBarsExtendToolbarColorBehindStatusBar() {
    int[] layouts = {
      R.layout.activity_qr, R.layout.profile_activity, R.layout.all_media_activity
    };
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : THEMES) {
                Context context = themedContext(theme);
                for (int layout : layouts) {
                  View screen = LayoutInflater.from(context).inflate(layout, null);
                  AppBarLayout appBar =
                      (AppBarLayout) screen.findViewById(R.id.toolbar).getParent();
                  assertEquals(
                      MaterialColors.getColor(context, R.attr.toolbar_color, 0),
                      appBar.getMaterialShapeBackground().getFillColor().getDefaultColor());
                }
              }
            });
  }

  @Test
  public void materialYouSelectionUsesDynamicThemesOnlyWhenSupported() {
    boolean available = DynamicColors.isDynamicColorAvailable();
    assertEquals(
        available ? R.style.TextSecure_MaterialYouLightTheme : R.style.TextSecure_LightTheme,
        new TestTheme().light(DynamicTheme.MATERIAL_YOU));
    assertEquals(
        available ? R.style.TextSecure_MaterialYouDarkTheme : R.style.TextSecure_DarkTheme,
        new TestTheme().dark(DynamicTheme.MATERIAL_YOU));
    assertEquals(
        available
            ? R.style.TextSecure_MaterialYouLightNoActionBar
            : R.style.TextSecure_LightNoActionBar,
        new TestNoActionBarTheme().light(DynamicTheme.MATERIAL_YOU));
    assertEquals(
        available
            ? R.style.TextSecure_MaterialYouDarkNoActionBar
            : R.style.TextSecure_DarkNoActionBar,
        new TestNoActionBarTheme().dark(DynamicTheme.MATERIAL_YOU));
    assertEquals(R.style.TextSecure_PurpleTheme, new TestTheme().light(DynamicTheme.PURPLE));
  }

  @Test
  public void materialYouUsesDynamicColorsForControlsInBothModes() {
    Assume.assumeTrue(DynamicColors.isDynamicColorAvailable());
    int[] themes = {
      R.style.TextSecure_MaterialYouLightNoActionBar,
      R.style.TextSecure_MaterialYouDarkNoActionBar
    };
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : themes) {
                Context context = DynamicColors.wrapContextIfAvailable(themedContext(theme));
                TypedValue applied = new TypedValue();
                assertTrue(
                    context
                        .getTheme()
                        .resolveAttribute(R.attr.isMaterial3DynamicColorApplied, applied, true));
                assertTrue(applied.data != 0);
                int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0);
                int onPrimary = MaterialColors.getColor(context, R.attr.colorOnPrimary, 0);
                assertEquals(primary, MaterialColors.getColor(context, R.attr.fab_color, 0));
                assertEquals(
                    onPrimary, MaterialColors.getColor(context, R.attr.fab_foreground_color, 0));
                assertTrue(ColorUtils.calculateContrast(onPrimary, primary) >= 4.5);
                assertTrue(
                    ColorUtils.calculateContrast(
                            MaterialColors.getColor(
                                context, R.attr.conversation_item_outgoing_text_primary_color, 0),
                            MaterialColors.getColor(
                                context, R.attr.conversation_item_outgoing_bubble_color, 0))
                        >= 4.5);

                FloatingActionButton fab =
                    LayoutInflater.from(context)
                        .inflate(R.layout.conversation_list_fragment, null)
                        .findViewById(R.id.fab);
                assertEquals(primary, fab.getBackgroundTintList().getDefaultColor());
                assertEquals(onPrimary, fab.getImageTintList().getDefaultColor());

                MaterialSwitch toggle =
                    (MaterialSwitch)
                        LayoutInflater.from(context)
                            .inflate(R.layout.switch_compat_preference, null);
                assertEquals(
                    primary,
                    toggle
                        .getTrackTintList()
                        .getColorForState(
                            new int[] {android.R.attr.state_enabled, android.R.attr.state_checked},
                            Color.TRANSPARENT));
                assertEquals(
                    primary,
                    MaterialColors.getColor(
                        new AlertDialog.Builder(context).getContext(), R.attr.colorPrimary, 0));
              }
            });
  }

  @Test
  public void materialYouIsAppliedDuringActivityCreationAndCanBeDisabled() {
    Assume.assumeTrue(DynamicColors.isDynamicColorAvailable());
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String previous = preferences.getString(Prefs.THEME_PREF, null);
    try {
      preferences.edit().putString(Prefs.THEME_PREF, DynamicTheme.MATERIAL_YOU).commit();
      try (ActivityScenario<WelcomeActivity> scenario =
          ActivityScenario.launch(WelcomeActivity.class)) {
        scenario.onActivity(activity -> assertDynamicColorsApplied(activity, true));
        preferences.edit().putString(Prefs.THEME_PREF, DynamicTheme.SYSTEM).commit();
        scenario.recreate();
        scenario.onActivity(activity -> assertDynamicColorsApplied(activity, false));
      }
    } finally {
      SharedPreferences.Editor editor = preferences.edit();
      if (previous == null) {
        editor.remove(Prefs.THEME_PREF);
      } else {
        editor.putString(Prefs.THEME_PREF, previous);
      }
      editor.commit();
    }
  }

  @Test
  public void materialYouKeepsNeutralWindowsAndReadableLightMessages() {
    Assume.assumeTrue(DynamicColors.isDynamicColorAvailable());
    int[] themes = {
      R.style.TextSecure_MaterialYouLightNoActionBar,
      R.style.TextSecure_MaterialYouDarkNoActionBar
    };
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int i = 0; i < themes.length; i++) {
                Context context = DynamicColors.wrapContextIfAvailable(themedContext(themes[i]));
                try (TypedArray attributes =
                    context.obtainStyledAttributes(new int[] {android.R.attr.windowBackground})) {
                  assertEquals(
                      i == 0 ? Color.WHITE : Color.BLACK, drawableColor(attributes.getDrawable(0)));
                }
                View row =
                    LayoutInflater.from(context).inflate(R.layout.conversation_list_item_view, null);
                assertEquals(Color.TRANSPARENT, drawableColor(row.getBackground()));
                row.setSelected(true);
                row.getBackground().jumpToCurrentState();
                assertEquals(
                    MaterialColors.getColor(context, R.attr.colorPrimaryContainer, 0),
                    drawableColor(row.getBackground()));
              }

              Context context = DynamicColors.wrapContextIfAvailable(themedContext(themes[0]));
              int bubble =
                  MaterialColors.getColor(
                      context, R.attr.conversation_item_outgoing_bubble_color, 0);
              int container = MaterialColors.getColor(context, R.attr.colorPrimaryContainer, 0);
              assertTrue(
                  ColorUtils.calculateLuminance(bubble) > ColorUtils.calculateLuminance(container));
              int forwarded = MaterialColors.getColor(context, R.attr.forwarded_message_color, 0);
              assertTrue(ColorUtils.calculateContrast(forwarded, bubble) >= 4.5);
              FrameLayout parent = new FrameLayout(context);
              LayoutInflater.from(context).inflate(R.layout.quote_view, parent);
              TextView quote = parent.findViewById(R.id.quote_text);
              assertTrue(ColorUtils.calculateContrast(quote.getCurrentTextColor(), bubble) >= 4.5);
            });
  }

  @Test
  public void materialYouSelectionAndMessageHighlightsUseWallpaperColors() {
    Assume.assumeTrue(DynamicColors.isDynamicColorAvailable());
    int[] themes = {
      R.style.TextSecure_MaterialYouLightNoActionBar,
      R.style.TextSecure_MaterialYouDarkNoActionBar
    };
    int[] backgrounds = {
      R.attr.conversation_list_item_background,
      R.attr.pinned_list_item_background,
      R.attr.conversation_item_background,
      R.attr.conversation_item_background_animated
    };
    int[] rows = {
      R.layout.account_selection_list_item,
      R.layout.contact_selection_list_item,
      R.layout.profile_document_item
    };
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : themes) {
                Context context = DynamicColors.wrapContextIfAvailable(themedContext(theme));
                int selected = MaterialColors.getColor(context, R.attr.colorPrimaryContainer, 0);
                for (int background : backgrounds) {
                  try (TypedArray attributes =
                      context.obtainStyledAttributes(new int[] {background})) {
                    Drawable drawable = attributes.getDrawable(0);
                    drawable.setState(new int[] {android.R.attr.state_selected});
                    drawable.jumpToCurrentState();
                    assertEquals(selected, drawableColor(drawable));
                  }
                }
                for (int rowLayout : rows) {
                  View row = LayoutInflater.from(context).inflate(rowLayout, null);
                  row.setSelected(true);
                  row.getBackground().jumpToCurrentState();
                  assertEquals(selected, drawableColor(row.getBackground()));
                }

                View gallery =
                    LayoutInflater.from(context).inflate(R.layout.profile_gallery_item, null);
                View indicator = gallery.findViewById(R.id.selected_indicator);
                int overlay = drawableColor(indicator.getBackground());
                int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0);
                ColorDrawable expectedOverlay =
                    new ColorDrawable(ColorUtils.setAlphaComponent(primary, Math.round(255 * 0.55f)));
                assertEquals(drawableColor(expectedOverlay), overlay);
                assertTrue(Color.alpha(overlay) > 0 && Color.alpha(overlay) < 255);
              }
            });
  }

  private static int drawableColor(Drawable drawable) {
    Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    drawable.setBounds(0, 0, 10, 10);
    drawable.draw(new Canvas(bitmap));
    int color = bitmap.getPixel(5, 5);
    bitmap.recycle();
    return color;
  }

  private static void assertDynamicColorsApplied(Context context, boolean expected) {
    TypedValue value = new TypedValue();
    boolean applied =
        context.getTheme().resolveAttribute(R.attr.isMaterial3DynamicColorApplied, value, true)
            && value.data != 0;
    assertEquals(expected, applied);
  }

  @Test
  public void lightToolbarsUseNeutralBackgroundAndReadableForeground() {
    int[] lightThemes = {
      R.style.TextSecure_LightTheme,
      R.style.TextSecure_PurpleTheme,
      R.style.TextSecure_GreenTheme,
      R.style.TextSecure_BlueTheme,
      R.style.TextSecure_RedTheme,
      R.style.TextSecure_PinkTheme,
      R.style.TextSecure_GrayTheme,
      R.style.TextSecure_MaterialYouLightTheme
    };
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : lightThemes) {
                Context context = themedContext(theme);
                if (theme == R.style.TextSecure_MaterialYouLightTheme) {
                  context = DynamicColors.wrapContextIfAvailable(context);
                }
                int background = MaterialColors.getColor(context, R.attr.toolbar_color, 0);
                int foreground = MaterialColors.getColor(context, R.attr.toolbar_foreground, 0);
                assertEquals(Color.WHITE, background);
                assertTrue(ColorUtils.calculateContrast(foreground, background) >= 4.5);

                View list =
                    LayoutInflater.from(context).inflate(R.layout.conversation_list_activity, null);
                assertEquals(
                    0,
                    list.findViewById(R.id.toolbar).getElevation(),
                    0.01f);
                TextView title = list.findViewById(R.id.toolbar_title);
                ImageView search = list.findViewById(R.id.search_action);
                assertEquals(foreground, title.getCurrentTextColor());
                assertEquals(foreground, search.getImageTintList().getDefaultColor());

                View chat =
                    LayoutInflater.from(context).inflate(R.layout.conversation_title_view, null);
                assertEquals(
                    foreground, ((TextView) chat.findViewById(R.id.title)).getCurrentTextColor());
                assertEquals(
                    foreground,
                    ((ImageView) chat.findViewById(R.id.up_button))
                        .getImageTintList()
                        .getDefaultColor());

                View media =
                    LayoutInflater.from(context).inflate(R.layout.all_media_activity, null);
                TabLayout tabs = media.findViewById(R.id.tab_layout);
                assertEquals(
                    foreground,
                    tabs.getTabTextColors()
                        .getColorForState(new int[] {android.R.attr.state_selected}, 0));
                assertTrue(
                    ColorUtils.calculateContrast(
                            tabs.getTabTextColors().getDefaultColor(), background)
                        >= 4.5);

                PopupMenu menu = new PopupMenu(context, new View(context));
                menu.inflate(R.menu.conversation);
                assertEquals(
                    foreground,
                    MenuItemCompat.getIconTintList(menu.getMenu().findItem(R.id.menu_all_media))
                        .getDefaultColor());
              }
            });
  }

  @Test
  public void toolbarTracksVisibleScrollContentAndStatusBar() {
    try (ActivityScenario<WelcomeActivity> scenario = ActivityScenario.launch(WelcomeActivity.class)) {
      ScrollView[] pages = new ScrollView[2];
      FrameLayout[] container = new FrameLayout[1];
      View[] bars = new View[2];
      ActionMode[] mode = new ActionMode[1];
      scenario.onActivity(
          activity -> {
            FrameLayout root = new FrameLayout(activity);
            container[0] = root;
            for (int i = 0; i < pages.length; i++) {
              ScrollView page = new ScrollView(activity);
              page.setTag(ToolbarScrollColors.SCROLL_TARGET);
              View content = new View(activity);
              page.addView(content, new ScrollView.LayoutParams(-1, 4000));
              root.addView(page, new FrameLayout.LayoutParams(-1, 300));
              pages[i] = page;
            }
            pages[1].setVisibility(View.GONE);
            bars[0] = new View(activity);
            bars[0].setId(R.id.toolbar);
            root.addView(bars[0], new FrameLayout.LayoutParams(1, 1));
            bars[1] = new View(activity);
            bars[1].setId(R.id.status_bar_background);
            root.addView(bars[1], new FrameLayout.LayoutParams(1, 1));
            activity.addContentView(root, new FrameLayout.LayoutParams(-1, -1));
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int top = MaterialColors.getColor(activity, R.attr.toolbar_color, 0);
            assertEquals(top, ((ColorDrawable) bars[0].getBackground()).getColor());
            assertEquals(top, ((ColorDrawable) bars[1].getBackground()).getColor());
            mode[0] =
                activity.startSupportActionMode(
                    new ActionMode.Callback() {
                      @Override
                      public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                        actionMode.setTitle("1");
                        return true;
                      }

                      @Override
                      public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                        return false;
                      }

                      @Override
                      public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
                        return false;
                      }

                      @Override
                      public void onDestroyActionMode(ActionMode actionMode) {}
                    });
            assertTrue(mode[0] != null);
            int surface = MaterialColors.getColor(activity, R.attr.colorSurfaceContainer, 0);
            assertEquals(surface, ((ColorDrawable) bars[0].getBackground()).getColor());
            assertEquals(surface, ((ColorDrawable) bars[1].getBackground()).getColor());
            mode[0].finish();
          });
      SystemClock.sleep(1000);
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int top = MaterialColors.getColor(activity, R.attr.toolbar_color, 0);
            assertEquals(top, ((ColorDrawable) bars[0].getBackground()).getColor());
            assertEquals(top, ((ColorDrawable) bars[1].getBackground()).getColor());
            pages[0].scrollTo(0, 100);
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int surface = MaterialColors.getColor(activity, R.attr.colorSurfaceContainer, 0);
            assertEquals(surface, ((ColorDrawable) bars[0].getBackground()).getColor());
            assertEquals(surface, ((ColorDrawable) bars[1].getBackground()).getColor());
            pages[0].setVisibility(View.GONE);
            pages[1].setVisibility(View.VISIBLE);
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int top = MaterialColors.getColor(activity, R.attr.toolbar_color, 0);
            assertEquals(top, ((ColorDrawable) bars[0].getBackground()).getColor());
            pages[1].scrollTo(0, 100);
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int surface = MaterialColors.getColor(activity, R.attr.colorSurfaceContainer, 0);
            assertEquals(surface, ((ColorDrawable) bars[0].getBackground()).getColor());
            pages[1].scrollTo(0, 0);
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int top = MaterialColors.getColor(activity, R.attr.toolbar_color, 0);
            assertEquals(top, ((ColorDrawable) bars[0].getBackground()).getColor());
            pages[1].removeAllViews();
            pages[1].addView(new View(activity), new ScrollView.LayoutParams(-1, 10));
            pages[1].setVisibility(View.GONE);
            pages[0].setVisibility(View.VISIBLE);
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int surface = MaterialColors.getColor(activity, R.attr.colorSurfaceContainer, 0);
            assertEquals(surface, ((ColorDrawable) bars[0].getBackground()).getColor());
            pages[0].setVisibility(View.GONE);
            pages[1].setVisibility(View.VISIBLE);
          });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            int top = MaterialColors.getColor(activity, R.attr.toolbar_color, 0);
            assertEquals(top, ((ColorDrawable) bars[0].getBackground()).getColor());
            container[0].removeAllViews();
          });
    }
  }

  @Test
  public void contextualActionBarUsesSurfaceAndToolbarForeground() {
    int[] menus = {
      R.menu.conversation_context,
      R.menu.conversation_list,
      R.menu.contact_list,
      R.menu.profile_context
    };
    int[] themes = {
      R.style.TextSecure_LightTheme,
      R.style.TextSecure_LightNoActionBar,
      R.style.TextSecure_DarkTheme,
      R.style.TextSecure_DarkNoActionBar,
      R.style.TextSecure_PurpleTheme,
      R.style.TextSecure_GreenTheme,
      R.style.TextSecure_BlueTheme,
      R.style.TextSecure_RedTheme,
      R.style.TextSecure_PinkTheme,
      R.style.TextSecure_GrayTheme,
      R.style.TextSecure_MaterialYouLightTheme,
      R.style.TextSecure_MaterialYouDarkTheme
    };
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : themes) {
                Context context = themedContext(theme);
                if (theme == R.style.TextSecure_MaterialYouLightTheme
                    || theme == R.style.TextSecure_MaterialYouDarkTheme) {
                  context = DynamicColors.wrapContextIfAvailable(context);
                }
                TypedValue actionModeTheme = new TypedValue();
                assertTrue(
                    context
                        .getTheme()
                        .resolveAttribute(
                            androidx.appcompat.R.attr.actionModeTheme, actionModeTheme, true));
                Context actionModeContext = new ContextThemeWrapper(context, actionModeTheme.resourceId);
                ActionBarContextView actionMode =
                    (ActionBarContextView)
                        LayoutInflater.from(actionModeContext)
                            .inflate(androidx.appcompat.R.layout.abc_action_mode_bar, null);
                int surface = MaterialColors.getColor(context, R.attr.colorSurfaceContainer, 0);
                int foreground = MaterialColors.getColor(context, R.attr.toolbar_foreground, 0);
                assertEquals(
                    surface,
                    ((ColorDrawable) actionMode.getBackground()).getColor());
                actionMode.setTitle("1");
                TextView title = actionMode.findViewById(androidx.appcompat.R.id.action_bar_title);
                assertEquals(foreground, title.getCurrentTextColor());
                assertEquals(
                    foreground,
                    MaterialColors.getColor(
                        actionMode.getContext(), androidx.appcompat.R.attr.colorControlNormal, 0));
                assertTrue(ColorUtils.calculateContrast(foreground, surface) >= 4.5);
                for (int menuId : menus) {
                  PopupMenu popup = new PopupMenu(actionModeContext, new View(actionModeContext));
                  popup.inflate(menuId);
                  Menu menu = popup.getMenu();
                  for (int i = 0; i < menu.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    if (item.getIcon() != null) {
                      assertEquals(foreground, MenuItemCompat.getIconTintList(item).getDefaultColor());
                    }
                  }
                }
              }
            });
  }

  private static Context themedContext(int theme) {
    Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
    return new ContextThemeWrapper(target, theme);
  }

  private static class TestTheme extends DynamicTheme {
    int light(String theme) {
      return getLightThemeStyle(theme);
    }

    int dark(String theme) {
      return getDarkThemeStyle(theme);
    }
  }

  private static class TestNoActionBarTheme extends DynamicNoActionBarTheme {
    int light(String theme) {
      return getLightThemeStyle(theme);
    }

    int dark(String theme) {
      return getDarkThemeStyle(theme);
    }
  }
}
