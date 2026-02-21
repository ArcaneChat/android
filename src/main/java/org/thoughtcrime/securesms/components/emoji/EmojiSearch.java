package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads emoji keyword data from assets and provides search functionality.
 * Used for emoji auto-completion when user types :query in the compose field.
 */
public class EmojiSearch {

  private static final String TAG = EmojiSearch.class.getSimpleName();
  private static final String ASSET_FILE = "emoji_keywords.json";
  private static final int MAX_RESULTS = 20;

  private static volatile EmojiSearch instance;

  private final List<String[]> entries; // each entry: [emoji, keyword1, keyword2, ...]

  private EmojiSearch(@NonNull List<String[]> entries) {
    this.entries = entries;
  }

  @NonNull
  public static EmojiSearch getInstance(@NonNull Context context) {
    if (instance == null) {
      synchronized (EmojiSearch.class) {
        if (instance == null) {
          instance = load(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  @NonNull
  @WorkerThread
  private static EmojiSearch load(@NonNull Context context) {
    List<String[]> entries = new ArrayList<>();
    try (InputStream is = context.getAssets().open(ASSET_FILE)) {
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
      String json = baos.toString("UTF-8");
      JSONArray array = new JSONArray(json);
      for (int i = 0; i < array.length(); i++) {
        JSONObject obj = array.getJSONObject(i);
        String emoji = obj.getString("e");
        JSONArray names = obj.getJSONArray("n");
        String[] entry = new String[names.length() + 1];
        entry[0] = emoji;
        for (int j = 0; j < names.length(); j++) {
          entry[j + 1] = names.getString(j);
        }
        entries.add(entry);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to load emoji keywords", e);
    }
    return new EmojiSearch(entries);
  }

  /**
   * Search for emojis whose names contain the given query (case-insensitive).
   * Returns up to MAX_RESULTS emoji strings.
   */
  @NonNull
  public List<String> search(@NonNull String query) {
    if (query.isEmpty()) return Collections.emptyList();
    String lowerQuery = query.toLowerCase();
    List<String> results = new ArrayList<>();
    for (String[] entry : entries) {
      for (int i = 1; i < entry.length; i++) {
        if (entry[i].contains(lowerQuery)) {
          results.add(entry[0]);
          break;
        }
      }
      if (results.size() >= MAX_RESULTS) break;
    }
    return results;
  }
}
