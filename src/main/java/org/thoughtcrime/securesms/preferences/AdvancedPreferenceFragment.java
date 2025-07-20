package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;
import static android.text.InputType.TYPE_TEXT_VARIATION_URI;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_BCC_SELF;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MVBOX_MOVE;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ONLY_FETCH_MVBOX;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_WEBXDC_REALTIME_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.getRpc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.b44t.messenger.rpc.RpcException;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.RegistrationActivity;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.proxy.ProxySettingsActivity;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.StreamUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;


public class AdvancedPreferenceFragment extends ListSummaryPreferenceFragment
                                        implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();

  private ListPreference showEmails;
  CheckBoxPreference bccSelfCheckbox;
  CheckBoxPreference mvboxMoveCheckbox;
  CheckBoxPreference onlyFetchMvboxCheckbox;
  CheckBoxPreference webxdcRealtimeCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    showEmails = (ListPreference) this.findPreference("pref_show_emails");
    if (showEmails != null) {
      showEmails.setOnPreferenceChangeListener((preference, newValue) -> {
        updateListSummary(preference, newValue);
        dcContext.setConfigInt(CONFIG_SHOW_EMAILS, Util.objectToInt(newValue));
        return true;
      });
    }

    Preference sendAsm = this.findPreference("pref_send_autocrypt_setup_message");
    if (sendAsm != null) {
      sendAsm.setOnPreferenceClickListener(new SendAsmListener());
    }

    bccSelfCheckbox = (CheckBoxPreference) this.findPreference("pref_bcc_self");
    if (bccSelfCheckbox != null) {
      bccSelfCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_BCC_SELF, enabled? 1 : 0);
        return true;
      });
    }

    mvboxMoveCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_move");
    if (mvboxMoveCheckbox != null) {
      mvboxMoveCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_MVBOX_MOVE, enabled? 1 : 0);
        return true;
      });
    }

    onlyFetchMvboxCheckbox = this.findPreference("pref_only_fetch_mvbox");
    if (onlyFetchMvboxCheckbox != null) {
      onlyFetchMvboxCheckbox.setOnPreferenceChangeListener(((preference, newValue) -> {
        final boolean enabled = (Boolean) newValue;
        if (enabled) {
          new AlertDialog.Builder(requireContext())
                  .setMessage(R.string.pref_imap_folder_warn_disable_defaults)
                  .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    dcContext.setConfigInt(CONFIG_ONLY_FETCH_MVBOX, 1);
                    ((CheckBoxPreference)preference).setChecked(true);
                  })
                  .setNegativeButton(R.string.cancel, null)
                  .show();
          return false;
        } else {
          dcContext.setConfigInt(CONFIG_ONLY_FETCH_MVBOX, 0);
          return true;
        }
      }));
    }

    webxdcRealtimeCheckbox = (CheckBoxPreference) this.findPreference("pref_webxdc_realtime_enabled");
    if (webxdcRealtimeCheckbox != null) {
      webxdcRealtimeCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_WEBXDC_REALTIME_ENABLED, enabled? 1 : 0);
        return true;
      });
    }

    Preference submitDebugLog = this.findPreference("pref_view_log");
    if (submitDebugLog != null) {
      submitDebugLog.setOnPreferenceClickListener(new ViewLogListener());
    }

    Preference webrtcInstance = this.findPreference("pref_webrtc_instance");
    if (webrtcInstance != null) {
      webrtcInstance.setOnPreferenceClickListener(new WebrtcInstanceListener());
    }
    updateWebrtcSummary();

    Preference webxdcStore = this.findPreference(Prefs.WEBXDC_STORE_URL_PREF);
    if (webxdcStore != null) {
      webxdcStore.setOnPreferenceClickListener(new WebxdcStoreUrlListener());
    }
    updateWebxdcStoreSummary();

    Preference developerModeEnabled = this.findPreference("pref_developer_mode_enabled");
    if (developerModeEnabled != null) {
      developerModeEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
        WebView.setWebContentsDebuggingEnabled((Boolean) newValue);
        return true;
      });
    }

    Preference selfReporting = this.findPreference("pref_self_reporting");
    if (selfReporting != null) {
      selfReporting.setOnPreferenceClickListener(((preference) -> {
        try {
          int chatId = getRpc(requireActivity()).draftSelfReport(dcContext.getAccountId());

          Intent intent = new Intent(requireActivity(), ConversationActivity.class);
          intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          requireActivity().startActivity(intent);
        } catch (RpcException e) {
          Log.e(TAG, "Error calling rpc.draftSelfReport()", e);
        }

        return true;
      }));
    }

    Preference proxySettings = this.findPreference("proxy_settings_button");
    if (proxySettings != null) {
      proxySettings.setOnPreferenceClickListener((preference) -> {
        startActivity(new Intent(requireActivity(), ProxySettingsActivity.class));
        return true;
      });
    }

    Preference passwordAndAccount = this.findPreference("password_account_settings_button");
    if (passwordAndAccount != null) {
      passwordAndAccount.setOnPreferenceClickListener(((preference) -> {
        boolean result = ScreenLockUtil.applyScreenLock(requireActivity(), getString(R.string.pref_password_and_account_settings), getString(R.string.enter_system_secret_to_continue), REQUEST_CODE_CONFIRM_CREDENTIALS_ACCOUNT);
        if (!result) {
          openRegistrationActivity();
        }
        return true;
      }));
    }

    if (dcContext.isChatmail()) {
      sendAsm.setVisible(false);
      showEmails.setVisible(false);
      bccSelfCheckbox.setVisible(false);
      mvboxMoveCheckbox.setVisible(false);
      onlyFetchMvboxCheckbox.setVisible(false);
    }
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_advanced);
  }

  @Override
  public void onResume() {
    super.onResume();
    Objects.requireNonNull(((ApplicationPreferencesActivity) requireActivity()).getSupportActionBar()).setTitle(R.string.menu_advanced);

    String value = Integer.toString(dcContext.getConfigInt("show_emails"));
    showEmails.setValue(value);
    updateListSummary(showEmails, value);

    bccSelfCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_BCC_SELF));
    mvboxMoveCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_MVBOX_MOVE));
    onlyFetchMvboxCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_ONLY_FETCH_MVBOX));
    webxdcRealtimeCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_WEBXDC_REALTIME_ENABLED));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_ACCOUNT) {
      openRegistrationActivity();
    }
  }

  protected File copyToCacheDir(Uri uri) throws IOException {
    try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri)) {
      File file = File.createTempFile("tmp-keys-file", ".tmp", requireActivity().getCacheDir());
      try (OutputStream outputStream = new FileOutputStream(file)) {
        StreamUtil.copy(inputStream, outputStream);
      }
      return file;
    }
  }

  public static @NonNull String getVersion(@Nullable Context context) {
    try {
      if (context == null) return "";

      String app     = context.getString(R.string.app_name);
      String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

      return String.format("%s %s", app, version);
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
      return context.getString(R.string.app_name);
    }
  }

  private class ViewLogListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      final Intent intent = new Intent(requireActivity(), LogViewActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class WebrtcInstanceListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      View gl = View.inflate(requireActivity(), R.layout.single_line_input, null);
      EditText inputField = gl.findViewById(R.id.input_field);
      inputField.setHint(R.string.videochat_instance_placeholder);
      inputField.setText(DcHelper.getVideochatURL(dcContext));
      inputField.setSelection(inputField.getText().length());
      inputField.setInputType(TYPE_TEXT_VARIATION_URI);
      new AlertDialog.Builder(requireActivity())
              .setTitle(R.string.videochat_instance)
              .setMessage(getString(R.string.videochat_instance_explain_2)+"\n\n"+getString(R.string.videochat_instance_example))
              .setView(gl)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                dcContext.setConfig(DcHelper.CONFIG_WEBRTC_INSTANCE, inputField.getText().toString());
                updateWebrtcSummary();
              })
              .show();
      return true;
    }
  }

  private class WebxdcStoreUrlListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      View gl = View.inflate(requireActivity(), R.layout.single_line_input, null);
      EditText inputField = gl.findViewById(R.id.input_field);
      inputField.setHint(Prefs.DEFAULT_WEBXDC_STORE_URL);
      inputField.setText(Prefs.getWebxdcStoreUrl(requireActivity()));
      inputField.setSelection(inputField.getText().length());
      inputField.setInputType(TYPE_TEXT_VARIATION_URI);
      new AlertDialog.Builder(requireActivity())
              .setTitle(R.string.webxdc_store_url)
              .setMessage(R.string.webxdc_store_url_explain)
              .setView(gl)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(android.R.string.ok, (dlg, btn) -> {
                Prefs.setWebxdcStoreUrl(requireActivity(), inputField.getText().toString());
                updateWebxdcStoreSummary();
              })
              .show();
      return true;
    }
  }

  private void updateWebrtcSummary() {
    Preference webrtcInstance = this.findPreference("pref_webrtc_instance");
    if (webrtcInstance != null) {
      webrtcInstance.setSummary(DcHelper.getVideochatURL(dcContext));
    }
  }

  private void updateWebxdcStoreSummary() {
    Preference preference = this.findPreference(Prefs.WEBXDC_STORE_URL_PREF);
    if (preference != null) {
        preference.setSummary(Prefs.getWebxdcStoreUrl(requireActivity()));
    }
  }

  private void openRegistrationActivity() {
    Intent intent = new Intent(requireActivity(), RegistrationActivity.class);
    startActivity(intent);
  }

  /***********************************************************************************************
   * Autocrypt
   **********************************************************************************************/

  private class SendAsmListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      Activity activity = requireActivity();
      new AlertDialog.Builder(activity)
        .setTitle(activity.getString(R.string.autocrypt_send_asm_title))
        .setMessage(activity.getString(R.string.autocrypt_send_asm_explain_before))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.autocrypt_send_asm_button, (dialog, which) -> {
              final String sc = dcContext.initiateKeyTransfer();
              if( sc != null ) {
                String scFormatted = "";
                try {
                  scFormatted = sc.substring(0, 4) + "  -  " + sc.substring(5, 9) + "  -  " + sc.substring(10, 14) + "  -\n\n" +
                      sc.substring(15, 19) + "  -  " + sc.substring(20, 24) + "  -  " + sc.substring(25, 29) + "  -\n\n" +
                      sc.substring(30, 34) + "  -  " + sc.substring(35, 39) + "  -  " + sc.substring(40, 44);
                } catch (Exception e) {
                  Log.e(TAG, "Unexpected exception", e);
                }
                new AlertDialog.Builder(activity)
                  .setTitle(activity.getString(R.string.autocrypt_send_asm_title))
                  .setMessage(activity.getString(R.string.autocrypt_send_asm_explain_after) + "\n\n" + scFormatted)
                  .setPositiveButton(android.R.string.ok, null)
                  .setCancelable(false) // prevent the dialog from being dismissed accidentally (when the dialog is closed, the setup code is gone forever and the user has to create a new setup message)
                  .show();
              }
        })
        .show();
      return true;
    }
  }

}
