package org.thoughtcrime.securesms.accounts;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import chat.delta.rpc.RpcException;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Util;

public class DeleteProfileDialog {
  private static final String TAG = "DeleteProfileDialog";

  public static void show(
      Activity activity, int accountId, Runnable onDelete, @Nullable Runnable onCancel) {
    View dialogView = View.inflate(activity, R.layout.dialog_delete_profile, null);
    AvatarView avatar = dialogView.findViewById(R.id.avatar);
    TextView nameView = dialogView.findViewById(R.id.name);
    TextView sizeView = dialogView.findViewById(R.id.size_label);
    TextView description = dialogView.findViewById(R.id.description);
    DcContext dcContext = DcHelper.getAccounts(activity).getAccount(accountId);
    DcContact contact = dcContext.getContact(DcContact.DC_CONTACT_ID_SELF);
    String name = dcContext.getConfig(DcHelper.CONFIG_DISPLAY_NAME);
    if (TextUtils.isEmpty(name)) {
      name = activity.getString(R.string.unnamed);
    }
    Recipient recipient = new Recipient(activity, contact, name);
    avatar.setAvatar(GlideApp.with(activity), recipient, false);
    nameView.setText(name);
    Util.runOnAnyBackgroundThread(
        () -> {
          try {
            final int sizeBytes = DcHelper.getRpc(activity).getAccountFileSize(accountId);
            Util.runOnMain(() -> sizeView.setText(Util.getPrettyFileSize(sizeBytes)));
          } catch (RpcException e) {
            Log.e(TAG, "Error calling rpc.getAccountFileSize()", e);
          }
        });
    description.setText(activity.getString(R.string.delete_account_explain_with_name, name));

    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle(R.string.delete_account)
            .setView(dialogView)
            .setNegativeButton(
                R.string.cancel,
                (d, which) -> {
                  if (onCancel != null) onCancel.run();
                })
            .setPositiveButton(R.string.delete, (d, which) -> onDelete.run())
            .show();
    Util.redPositiveButton(dialog);
  }
}
