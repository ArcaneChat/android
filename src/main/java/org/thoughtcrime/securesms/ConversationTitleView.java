package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;

public class ConversationTitleView extends RelativeLayout {

  private View            content;
  private ImageView       back;
  private AvatarView      avatar;
  private TextView        title;
  private TextView        subtitle;
  private ImageView       ephemeralIcon;

  public ConversationTitleView(Context context) {
    this(context, null);
  }

  public ConversationTitleView(Context context, AttributeSet attrs) {
    super(context, attrs);

  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    this.back          = ViewUtil.findById(this, R.id.up_button);
    this.content       = ViewUtil.findById(this, R.id.content);
    this.title         = ViewUtil.findById(this, R.id.title);
    this.subtitle      = ViewUtil.findById(this, R.id.subtitle);
    this.avatar        = ViewUtil.findById(this, R.id.avatar);
    this.ephemeralIcon = ViewUtil.findById(this, R.id.ephemeral_icon);

    ViewUtil.setTextViewGravityStart(this.title, getContext());
    ViewUtil.setTextViewGravityStart(this.subtitle, getContext());
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcChat dcChat) {
    setTitle(glideRequests, dcChat, false);
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcChat dcChat, boolean profileView) {
    final int chatId = dcChat.getId();
    final Context context = getContext();
    final DcContext dcContext = DcHelper.getContext(context);

    // set title and subtitle texts
    title.setText(dcChat.getName());
    String subtitleStr = null;

    boolean isOnline = false;
    int[] chatContacts = dcContext.getChatContacts(chatId);
    if (dcChat.isMailingList()) {
      subtitleStr = dcChat.getMailinglistAddr();
      if (!profileView) {
        if (TextUtils.isEmpty(subtitleStr)) {
          subtitleStr = context.getString(R.string.channel);
        } else {
          subtitleStr = context.getString(R.string.super_group);
        }
      }
    } else if (dcChat.isInBroadcast()) {
      subtitleStr = context.getString(R.string.channel);
    } else if (dcChat.isOutBroadcast()) {
      if (!profileView) {
        subtitleStr = context.getResources().getQuantityString(R.plurals.n_recipients, chatContacts.length, chatContacts.length);
      }
    } else if( dcChat.isMultiUser() ) {
      if (!profileView) {
        subtitleStr = context.getResources().getQuantityString(R.plurals.n_members, chatContacts.length, chatContacts.length);
      }
    } else if( chatContacts.length>=1 ) {
      if( dcChat.isSelfTalk() ) {
        subtitleStr = context.getString(R.string.chat_self_talk_subtitle);
      }
      else if( dcChat.isDeviceTalk() ) {
        subtitleStr = context.getString(R.string.device_talk_subtitle);
      }
      else {
        DcContact dcContact = dcContext.getContact(chatContacts[0]);
        isOnline = dcContact.wasSeenRecently();
        if (profileView || !dcChat.isEncrypted()) {
          subtitleStr = dcContact.getAddr();
        } else if (dcContact.isBot()) {
          subtitleStr = context.getString(R.string.bot);
        } else if (isOnline) {
          subtitleStr = context.getString(R.string.online);
        } else {
          long timestamp = dcContact.getLastSeen();
          if (timestamp >= 0) {
            subtitleStr = context.getString(R.string.last_seen_at, DateUtils.getExtendedTimeSpanString(context, timestamp));
          }
        }
      }
    }

    avatar.setAvatar(glideRequests, new Recipient(getContext(), dcChat), false);
    avatar.setSeenRecently(isOnline);
    int imgLeft = dcChat.isMuted()? R.drawable.ic_volume_off_white_18dp : 0;
    int imgRight = dcChat.isSelfTalk() || dcChat.isDeviceTalk()? R.drawable.ic_verified : 0;
    title.setCompoundDrawablesWithIntrinsicBounds(imgLeft, 0, imgRight, 0);
    if (!TextUtils.isEmpty(subtitleStr)) {
      subtitle.setText(subtitleStr);
      subtitle.setVisibility(View.VISIBLE);
    } else {
      subtitle.setVisibility(View.GONE);
    }
    boolean isEphemeral = dcContext.getChatEphemeralTimer(chatId) != 0;
    ephemeralIcon.setVisibility(isEphemeral? View.VISIBLE : View.GONE);
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcContact contact) {
    // This function is only called for contacts without a corresponding 1:1 chat.
    // If there is a 1:1 chat, then the overloaded function
    // setTitle(GlideRequests, DcChat, boolean) is called.
    avatar.setAvatar(glideRequests, new Recipient(getContext(), contact), false);
    avatar.setSeenRecently(contact.wasSeenRecently());

    title.setText(contact.getDisplayName());
    subtitle.setText(contact.getAddr());
    subtitle.setVisibility(View.VISIBLE);
  }

  public void setSeenRecently(boolean seenRecently) {
    avatar.setSeenRecently(seenRecently);
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener listener) {
    this.content.setOnClickListener(listener);
    this.avatar.setAvatarClickListener(listener);
  }

  public void setOnBackClickedListener(@Nullable OnClickListener listener) {
    this.back.setOnClickListener(listener);
  }
}
