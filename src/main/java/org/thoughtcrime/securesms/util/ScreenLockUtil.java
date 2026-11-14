package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class ScreenLockUtil {

  public static boolean applyScreenLock(
      Activity activity, String title, String descr, ActivityResultLauncher<Intent> launcher) {
    KeyguardManager keyguardManager =
        (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
    Intent intent;
    if (keyguardManager != null) {
      intent = keyguardManager.createConfirmDeviceCredentialIntent(title, descr);
      if (intent != null) {
        launcher.launch(intent);
        return true;
      }
    }
    return false;
  }

  /**
   * Runs an action only after the user confirms with the device lock (or immediately, if no lock
   * screen is configured).
   *
   * <p>Create one instance per activity, before it is started (e.g. in onCreate).
   */
  public static class AuthorizedRunner {
    private final Activity activity;
    private final ActivityResultLauncher<Intent> launcher;
    private Runnable pendingAction;

    public AuthorizedRunner(ComponentActivity activity) {
      this.activity = activity;
      this.launcher =
          activity.registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(),
              result -> {
                Runnable action = pendingAction;
                pendingAction = null;
                if (result.getResultCode() == Activity.RESULT_OK && action != null) {
                  action.run();
                }
              });
    }

    public void run(String title, String descr, Runnable action) {
      pendingAction = action;
      if (!applyScreenLock(activity, title, descr, launcher)) {
        pendingAction = null;
        action.run();
      }
    }
  }
}
