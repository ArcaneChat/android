package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BlockedContactsActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

public class PrivacyPreferenceFragment extends ListSummaryPreferenceFragment {
  private static final String TAG = PrivacyPreferenceFragment.class.getSimpleName();

  private CheckBoxPreference readReceiptsCheckbox;

  private ListPreference autoDelDevice;
  private ListPreference autoDelServer;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    readReceiptsCheckbox = (CheckBoxPreference) this.findPreference("pref_read_receipts");
    readReceiptsCheckbox.setOnPreferenceChangeListener(new ReadReceiptToggleListener());

    this.findPreference("preference_category_blocked").setOnPreferenceClickListener(new BlockedContactsClickListener());

    autoDelDevice = findPreference("autodel_device");
    autoDelDevice.setOnPreferenceChangeListener(new AutodelChangeListener("delete_device_after"));

    autoDelServer = findPreference("autodel_server");
    autoDelServer.setOnPreferenceChangeListener(new AutodelChangeListener("delete_server_after"));
    if (dcContext.isChatmail()) {
      autoDelServer.setVisible(false);
    }

    Preference screenSecurity = this.findPreference(Prefs.SCREEN_SECURITY_PREF);
    screenSecurity.setOnPreferenceChangeListener(new ScreenShotSecurityListener());
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_privacy);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity)getActivity()).getSupportActionBar().setTitle(R.string.pref_privacy);

    readReceiptsCheckbox.setChecked(0 != dcContext.getConfigInt("mdns_enabled"));
    initAutodelFromCore();
  }

  private void initAutodelFromCore() {
    String value = Integer.toString(dcContext.getConfigInt("delete_server_after"));
    autoDelServer.setValue(value);
    updateListSummary(autoDelServer, value, (value.equals("0") || dcContext.isChatmail())? null : getString(R.string.autodel_server_enabled_hint));

    value = Integer.toString(dcContext.getConfigInt("delete_device_after"));
    autoDelDevice.setValue(value);
    updateListSummary(autoDelDevice, value);
  }

  public static CharSequence getSummary(Context context) {
    DcContext dcContext = DcHelper.getContext(context);
    final String onRes = context.getString(R.string.on);
    final String offRes = context.getString(R.string.off);
    String readReceiptState = dcContext.getConfigInt("mdns_enabled")!=0? onRes : offRes;
    return context.getString(R.string.pref_read_receipts) + " " + readReceiptState;
  }

  private class BlockedContactsClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      Intent intent = new Intent(getActivity(), BlockedContactsActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class ReadReceiptToggleListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      boolean enabled = (boolean) newValue;
      dcContext.setConfigInt("mdns_enabled", enabled ? 1 : 0);
      return true;
    }
  }

  private class AutodelChangeListener implements Preference.OnPreferenceChangeListener {
    private final String coreKey;

    AutodelChangeListener(String coreKey) {
      this.coreKey = coreKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      int timeout = Util.objectToInt(newValue);
      Context context = preference.getContext();
      boolean fromServer = coreKey.equals("delete_server_after");
      if (timeout>0 && !(fromServer && dcContext.isChatmail())) {
        int delCount = DcHelper.getContext(context).estimateDeletionCount(fromServer, timeout);

        View gl = View.inflate(getActivity(), R.layout.dialog_with_checkbox, null);
        CheckBox confirmCheckbox = gl.findViewById(R.id.dialog_checkbox);
        TextView msg = gl.findViewById(R.id.dialog_message);

        // If we'd use both `setMessage()` and `setView()` on the same AlertDialog, on small screens the
        // "OK" and "Cancel" buttons would not be show. So, put the message into our custom view:
        msg.setText(String.format(context.getString(fromServer?
                R.string.autodel_server_ask : R.string.autodel_device_ask),
                delCount, getSelectedSummary(preference, newValue)));
        confirmCheckbox.setText(R.string.autodel_confirm);

        new AlertDialog.Builder(context)
                .setTitle(preference.getTitle())
                .setView(gl)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                  if (confirmCheckbox.isChecked()) {
                    dcContext.setConfigInt(coreKey, timeout);
                    initAutodelFromCore();
                  } else {
                    onPreferenceChange(preference, newValue);
                  }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> initAutodelFromCore())
                .setCancelable(true) // Enable the user to quickly cancel if they are intimidated by the warnings :)
                .setOnCancelListener(dialog -> initAutodelFromCore())
                .show();
      } else if (fromServer && timeout == 1 /*at once, using a constant that cannot be used in .xml would weaken grep ability*/) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.autodel_server_warn_multi_device_title)
                .setMessage(R.string.autodel_server_warn_multi_device)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                  dcContext.setConfigInt(coreKey, timeout);
                  initAutodelFromCore();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> initAutodelFromCore())
                .setCancelable(true)
                .setOnCancelListener(dialog -> initAutodelFromCore())
                .show();
      } else {
        updateListSummary(preference, newValue);
        dcContext.setConfigInt(coreKey, timeout);
      }
      return true;
    }
  }

  private class ScreenShotSecurityListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      boolean enabled = (Boolean) newValue;
      Prefs.setScreenSecurityEnabled(getContext(), enabled);
      Toast.makeText(getContext(), R.string.pref_screen_security_please_restart_hint, Toast.LENGTH_LONG).show();
      return true;
    }
  }
}
