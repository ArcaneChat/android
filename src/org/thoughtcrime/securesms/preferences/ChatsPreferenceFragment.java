package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;

public class ChatsPreferenceFragment extends ListSummaryPreferenceFragment {
  private ListPreference mediaQuality;
  private ListPreference autoDownload;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    mediaQuality = (ListPreference) this.findPreference("pref_compression");
    mediaQuality.setOnPreferenceChangeListener((preference, newValue) -> {
      updateListSummary(preference, newValue);
      dcContext.setConfigInt(DcHelper.CONFIG_MEDIA_QUALITY, Util.objectToInt(newValue));
      return true;
    });


    autoDownload = findPreference("auto_download");
    autoDownload.setOnPreferenceChangeListener((preference, newValue) -> {
      updateListSummary(preference, newValue);
      dcContext.setConfigInt("download_limit", Util.objectToInt(newValue));
      return true;
    });
    nicerAutoDownloadNames();

    Preference backup = this.findPreference("pref_backup");
    backup.setOnPreferenceClickListener(new BackupListener());
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_chats);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity)getActivity()).getSupportActionBar().setTitle(R.string.pref_chats_and_media);

    String value = Integer.toString(dcContext.getConfigInt(DcHelper.CONFIG_MEDIA_QUALITY));
    mediaQuality.setValue(value);
    updateListSummary(mediaQuality, value);

    value = Integer.toString(dcContext.getConfigInt("download_limit"));
    value = alignToMaxEntry(value, autoDownload.getEntryValues());
    autoDownload.setValue(value);
    updateListSummary(autoDownload, value);
  }

  // prefixes "Up to ..." to all entry names but the first one.
  private void nicerAutoDownloadNames() {
    CharSequence[] entries = autoDownload.getEntries();
    for (int i = 1 /*skip first*/; i < entries.length; i++) {
      if (entries[i].equals("160 KiB")) {
        entries[i] = getString(R.string.up_to_x_most_worse_quality_images, entries[i]);
      } else if (entries[i].equals("640 KiB")) {
        entries[i] = getString(R.string.up_to_x_most_balanced_quality_images, entries[i]);
      } else {
        entries[i] = getString(R.string.up_to_x, entries[i]);
      }
    }
    autoDownload.setEntries(entries);
  }

  // Assumes `entryValues` are sorted smallest (index 0) to largest (last index)
  // and returns the an item close to `selectedValue`.
  private String alignToMaxEntry(@NonNull String selectedValue, @NonNull CharSequence[] entryValues) {
    try {
      int selectedValueInt = Integer.parseInt(selectedValue);
      for (int i = entryValues.length - 1; i >= 1 /*first is returned below*/; i--) {
        int entryValueMin = i == 1 ? (Integer.parseInt(entryValues[i - 1].toString()) + 1) : Integer.parseInt(entryValues[i].toString());
        if (selectedValueInt >= entryValueMin) {
          return entryValues[i].toString();
        }
      }
      return entryValues[0].toString();
    } catch(Exception e) {
      return selectedValue;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP) {
      performBackup();
    }
  }

  public static CharSequence getSummary(Context context) {
    final String quality;
    if (Prefs.isHardCompressionEnabled(context)) {
      quality = context.getString(R.string.pref_outgoing_worse);
    } else {
      quality = context.getString(R.string.pref_outgoing_balanced);
    }
    return context.getString(R.string.pref_outgoing_media_quality) + " " + quality;
  }

  /***********************************************************************************************
   * Backup
   **********************************************************************************************/

  private class BackupListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      boolean result = ScreenLockUtil.applyScreenLock(getActivity(), getString(R.string.pref_backup), getString(R.string.enter_system_secret_to_continue), REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP);
      if (!result) {
        performBackup();
      }
      return true;
    }
  }

  private void performBackup() {
    Permissions.with(getActivity())
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) // READ_EXTERNAL_STORAGE required to read folder contents and to generate backup names
            .alwaysGrantOnSdk30()
            .ifNecessary()
            .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
            .onAllGranted(() -> {
              final String addr = DcHelper.get(getActivity(), DcHelper.CONFIG_CONFIGURED_ADDRESS);
              AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                      .setTitle(R.string.pref_backup)
                      .setMessage(R.string.pref_backup_export_explain)
                      .setNeutralButton(android.R.string.cancel, null)
                      .setPositiveButton(getActivity().getString(R.string.pref_backup_export_x, addr), (dialogInterface, i) -> startImexOne(DcContext.DC_IMEX_EXPORT_BACKUP));
              int[] allAccounts = DcHelper.getAccounts(getActivity()).getAll();
              if (allAccounts.length > 1) {
                String exportAllString = getActivity().getString(R.string.pref_backup_export_all, allAccounts.length);
                builder.setNegativeButton(exportAllString, (dialogInterface, i) -> startImexAll(DcContext.DC_IMEX_EXPORT_BACKUP));
              }
              builder.show();
            })
            .execute();
  }
}
