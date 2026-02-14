package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.emoji2.emojipicker.EmojiPickerView;
import androidx.emoji2.emojipicker.EmojiViewItem;

import com.google.android.material.tabs.TabLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.InputAwareLayout.InputView;

import java.io.File;

public class MediaKeyboard extends LinearLayout implements InputView, Consumer<EmojiViewItem>, StickerPickerView.StickerPickerListener {

  private static final String TAG = MediaKeyboard.class.getSimpleName();

  @Nullable private MediaKeyboardListener keyboardListener;
  private EmojiPickerView emojiPicker;
  private StickerPickerView stickerPicker;
  private TabLayout tabLayout;

  public MediaKeyboard(@NonNull Context context) {
    super(context);
  }

  public MediaKeyboard(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setKeyboardListener(@Nullable MediaKeyboardListener listener) {
    this.keyboardListener = listener;
  }

  @Override
  public boolean isShowing() {
    return getVisibility() == VISIBLE;
  }

  @Override
  public void show(int height, boolean immediate) {
    ViewGroup.LayoutParams params = getLayoutParams();
    params.height = height;
    Log.i(TAG, "showing emoji drawer with height " + params.height);
    setLayoutParams(params);

    show();
  }

  public void show() {
    if (emojiPicker == null) {
      setupViews();
    }
    setVisibility(VISIBLE);
    if (keyboardListener != null) keyboardListener.onShown();
  }

  private void setupViews() {
    emojiPicker = findViewById(R.id.emoji_picker);
    stickerPicker = findViewById(R.id.sticker_picker);
    tabLayout = findViewById(R.id.media_keyboard_tabs);

    if (emojiPicker != null) {
      emojiPicker.setOnEmojiPickedListener(this);
    }

    if (stickerPicker != null) {
      stickerPicker.setStickerPickerListener(this);
    }

    if (tabLayout != null) {
      tabLayout.addTab(tabLayout.newTab().setText(R.string.emoji));
      tabLayout.addTab(tabLayout.newTab().setText(R.string.sticker));

      tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
          if (tab.getPosition() == 0) {
            showEmojiPicker();
          } else {
            showStickerPicker();
          }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
      });
    }
  }

  private void showEmojiPicker() {
    if (emojiPicker != null) {
      emojiPicker.setVisibility(View.VISIBLE);
    }
    if (stickerPicker != null) {
      stickerPicker.setVisibility(View.GONE);
    }
  }

  private void showStickerPicker() {
    if (emojiPicker != null) {
      emojiPicker.setVisibility(View.GONE);
    }
    if (stickerPicker != null) {
      stickerPicker.setVisibility(View.VISIBLE);
      stickerPicker.loadStickers();
    }
  }

  @Override
  public void hide(boolean immediate) {
    setVisibility(GONE);
    if (keyboardListener != null) keyboardListener.onHidden();
    Log.i(TAG, "hide()");
  }

  @Override
  public void accept(EmojiViewItem emojiViewItem) {
    if (keyboardListener != null) keyboardListener.onEmojiPicked(emojiViewItem.getEmoji());
  }

  @Override
  public void onStickerSelected(@NonNull File stickerFile) {
    if (keyboardListener != null) {
      keyboardListener.onStickerPicked(Uri.fromFile(stickerFile));
    }
  }

  public interface MediaKeyboardListener {
    void onShown();
    void onHidden();
    void onEmojiPicked(String emoji);
    void onStickerPicked(Uri stickerUri);
  }
}
