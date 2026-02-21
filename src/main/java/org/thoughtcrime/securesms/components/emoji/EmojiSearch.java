package org.thoughtcrime.securesms.components.emoji;

import androidx.annotation.NonNull;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides emoji search functionality using the emoji-java library.
 * Used for emoji auto-completion when user types :query in the compose field.
 */
public final class EmojiSearch {

  private static final int MAX_RESULTS = 20;

  private EmojiSearch() {}

  /**
   * Search for emojis whose aliases or tags contain the given query (case-insensitive).
   * Returns up to MAX_RESULTS emoji Unicode strings.
   */
  @NonNull
  public static List<String> search(@NonNull String query) {
    if (query.isEmpty()) return Collections.emptyList();
    String lowerQuery = query.toLowerCase();
    List<String> results = new ArrayList<>();
    for (Emoji emoji : EmojiManager.getAll()) {
      if (matchesQuery(emoji, lowerQuery)) {
        results.add(emoji.getUnicode());
        if (results.size() >= MAX_RESULTS) break;
      }
    }
    return results;
  }

  private static boolean matchesQuery(@NonNull Emoji emoji, @NonNull String lowerQuery) {
    for (String alias : emoji.getAliases()) {
      if (alias.toLowerCase().contains(lowerQuery)) return true;
    }
    for (String tag : emoji.getTags()) {
      if (tag.toLowerCase().contains(lowerQuery)) return true;
    }
    return false;
  }
}
