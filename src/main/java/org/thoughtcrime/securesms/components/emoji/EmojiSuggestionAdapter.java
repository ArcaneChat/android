package org.thoughtcrime.securesms.components.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for showing emoji auto-completion suggestions.
 */
public class EmojiSuggestionAdapter extends RecyclerView.Adapter<EmojiSuggestionAdapter.EmojiViewHolder> {

  public interface OnEmojiClickListener {
    void onEmojiClicked(@NonNull String emoji);
  }

  private final List<String> emojis = new ArrayList<>();
  private OnEmojiClickListener listener;

  public void setOnEmojiClickListener(OnEmojiClickListener listener) {
    this.listener = listener;
  }

  public void setEmojis(@NonNull List<String> newEmojis) {
    int oldSize = emojis.size();
    emojis.clear();
    emojis.addAll(newEmojis);
    int newSize = emojis.size();
    if (oldSize > 0) notifyItemRangeRemoved(0, oldSize);
    if (newSize > 0) notifyItemRangeInserted(0, newSize);
  }

  @NonNull
  @Override
  public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.emoji_suggestion_item, parent, false);
    return new EmojiViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
    String emoji = emojis.get(position);
    holder.emojiView.setText(emoji);
    holder.itemView.setOnClickListener(v -> {
      if (listener != null) listener.onEmojiClicked(emoji);
    });
  }

  @Override
  public int getItemCount() {
    return emojis.size();
  }

  static class EmojiViewHolder extends RecyclerView.ViewHolder {
    final TextView emojiView;

    EmojiViewHolder(@NonNull View itemView) {
      super(itemView);
      emojiView = itemView.findViewById(R.id.emoji_text);
    }
  }
}
