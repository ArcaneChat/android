package org.thoughtcrime.securesms.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ActionBarContextView;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceViewHolder;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.SwitchPreferenceCompat;

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
                PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(toggle);

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
  public void contextualActionBarUsesBrandedBackgroundAndWhiteText() {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int theme : THEMES) {
                Context context = themedContext(theme);
                assertEquals(
                    MaterialColors.getColor(
                        context, androidx.appcompat.R.attr.actionModeBackground, 0),
                    MaterialColors.getColor(context, R.attr.toolbar_color, 0));
                assertTrue(
                    ColorUtils.calculateContrast(
                            MaterialColors.getColor(context, R.attr.colorOnPrimary, 0),
                            MaterialColors.getColor(context, R.attr.colorPrimary, 0))
                        >= 4.5);
                ActionBarContextView actionMode =
                    (ActionBarContextView)
                        LayoutInflater.from(context)
                            .inflate(androidx.appcompat.R.layout.abc_action_mode_bar, null);
                assertEquals(
                    MaterialColors.getColor(
                        context, androidx.appcompat.R.attr.actionModeBackground, 0),
                    ((ColorDrawable) actionMode.getBackground()).getColor());
                actionMode.setTitle("1");
                TextView title = actionMode.findViewById(androidx.appcompat.R.id.action_bar_title);
                assertEquals(Color.WHITE, title.getCurrentTextColor());
                assertEquals(
                    Color.WHITE,
                    MaterialColors.getColor(
                        actionMode.getContext(), androidx.appcompat.R.attr.colorControlNormal, 0));
              }
            });
  }

  private static Context themedContext(int theme) {
    Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
    return new ContextThemeWrapper(target, theme);
  }
}
