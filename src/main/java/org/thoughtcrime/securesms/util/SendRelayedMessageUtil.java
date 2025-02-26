package org.thoughtcrime.securesms.util;

import static org.thoughtcrime.securesms.util.RelayUtil.getForwardedMessageIDs;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedText;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedSubject;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedHtml;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedType;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedUris;
import static org.thoughtcrime.securesms.util.RelayUtil.isForwarding;
import static org.thoughtcrime.securesms.util.RelayUtil.isSharing;
import static org.thoughtcrime.securesms.util.RelayUtil.resetRelayingMessageContent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.provider.OpenableColumns;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationListRelayingActivity;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class SendRelayedMessageUtil {
  private static final String TAG = SendRelayedMessageUtil.class.getSimpleName();

  public static void immediatelyRelay(Activity activity, int chatId) {
    immediatelyRelay(activity, new Long[]{(long) chatId});
  }

  public static void immediatelyRelay(Activity activity, final Long[] chatIds) {
    ConversationListRelayingActivity.finishActivity();
    if (isForwarding(activity)) {
      int[] forwardedMessageIDs = getForwardedMessageIDs(activity);
      resetRelayingMessageContent(activity);
      if (forwardedMessageIDs == null) return;

      Util.runOnAnyBackgroundThread(() -> {
        DcContext dcContext = DcHelper.getContext(activity);
        for (long longChatId : chatIds) {
          int chatId = (int) longChatId;
          if (dcContext.getChat(chatId).isSelfTalk()) {
            for (int msgId : forwardedMessageIDs) {
              DcMsg msg = dcContext.getMsg(msgId);
              if (msg.canSave() && msg.getSavedMsgId() == 0 && msg.getChatId() != chatId) {
                dcContext.saveMsgs(new int[]{msgId});
              } else {
                handleForwarding(activity, chatId, new int[]{msgId});
              }
            }
          } else {
            handleForwarding(activity, chatId, forwardedMessageIDs);
          }
        }

      });
    } else if (isSharing(activity)) {
      ArrayList<Uri> sharedUris = getSharedUris(activity);
      String sharedText = getSharedText(activity);
      String subject = getSharedSubject(activity);
      String sharedHtml = getHtml(activity, getSharedHtml(activity));
      String msgType = getSharedType(activity);
      resetRelayingMessageContent(activity);
      Util.runOnAnyBackgroundThread(() -> {
        for (long chatId : chatIds) {
          sendMultipleMsgs(activity, (int) chatId, sharedUris, msgType, sharedHtml, subject, sharedText);
        }
      });
    }
  }

  private static void handleForwarding(Context context, int chatId, int[] forwardedMessageIDs) {
    DcContext dcContext = DcHelper.getContext(context);
    dcContext.forwardMsgs(forwardedMessageIDs, chatId);
  }

  public static void sendMultipleMsgs(Context context, int chatId, ArrayList<Uri> sharedUris, String sharedText) {
    sendMultipleMsgs(context, chatId, sharedUris, null, null, null, sharedText);
  }

  private static void sendMultipleMsgs(Context context, int chatId, ArrayList<Uri> sharedUris, String msgType, String sharedHtml, String subject, String sharedText) {
    DcContext dcContext = DcHelper.getContext(context);
    ArrayList<Uri> uris = sharedUris;
    String text = sharedText;

    if (uris.size() == 1) {
      dcContext.sendMsg(chatId, createMessage(context, uris.get(0), msgType, sharedHtml, subject, text));
    } else {
      if (text != null || sharedHtml != null) {
        dcContext.sendMsg(chatId, createMessage(context, null, null, sharedHtml, subject, text));
      }
      for (Uri uri : uris) {
        dcContext.sendMsg(chatId, createMessage(context, uri, null, null, subject, null));
      }
    }
  }

  public static boolean containsVideoType(Context context, ArrayList<Uri> uris) {
    for (final Uri uri : uris) {
      final String mimeType = MediaUtil.getMimeType(context, uri);
      if (MediaUtil.isVideoType(mimeType)) {
        return true;
      }
    }
    return false;
  }

  public static DcMsg createMessage(Context context, Uri uri, String type, String html, String subject, String text) throws NullPointerException {
    DcContext dcContext = DcHelper.getContext(context);
    DcMsg message;
    String mimeType = MediaUtil.getMimeType(context, uri);
    if (uri == null) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    } else if ("sticker".equals(type)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_STICKER);
      message.forceSticker();
    } else if ("image".equals(type) || MediaUtil.isImageType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_IMAGE);
    } else if ("audio".equals(type) || MediaUtil.isAudioType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_AUDIO);
    } else if ("video".equals(type) || MediaUtil.isVideoType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_VIDEO);
    } else {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_FILE);
    }

    if (uri != null) {
      setFileFromUri(context, uri, message, mimeType);
    }
    if (html != null) {
      message.setHtml(html);
    }
    if (subject != null) {
      message.setSubject(subject);
    }
    if (text != null) {
      message.setText(text);
    }
    return message;
  }

  private static void setFileFromUri(Context context, Uri uri, DcMsg message, String mimeType) {
    String path;
    DcContext dcContext = DcHelper.getContext(context);
    String filename = "cannot-resolve.jpg"; // best guess, this still leads to most images being workable if OS does weird things
    try {

      if (PartAuthority.isLocalUri(uri)) {
        filename = uri.getPathSegments().get(PersistentBlobProvider.FILENAME_PATH_SEGMENT);
      } else if (uri.getScheme().equals("content")) {
        final ContentResolver contentResolver = context.getContentResolver();
        final Cursor cursor = contentResolver.query(uri, null, null, null, null);
        try {
          if (cursor != null && cursor.moveToFirst()) {
            final int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex >= 0) {
              filename = cursor.getString(nameIndex);
            }
          }
        } finally {
          cursor.close();
        }
      }

      path = DcHelper.getBlobdirFile(dcContext, filename, "temp");

      // copy content to this file
      if (path != null) {
        InputStream inputStream = PartAuthority.getAttachmentStream(context, uri);
        OutputStream outputStream = new FileOutputStream(path);
        Util.copy(inputStream, outputStream);
      }
    } catch (Exception e) {
      e.printStackTrace();
      path = null;
    }
    message.setFileAndDeduplicate(path, filename, mimeType);
  }

  private static String getHtml(Context context, Uri uri) {
    try {
      InputStream in = PartAuthority.getAttachmentStream(context, uri);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      StringBuilder html = new StringBuilder();
      for (String line; (line = br.readLine()) != null; ) {
        html.append(line).append('\n');
      }
      if (in != null) in.close();
      br.close();
      return html.toString();
    } catch (Exception ex) {
      Log.e(TAG, "failed to get HTML", ex);
      return null;
    }
  }
}
