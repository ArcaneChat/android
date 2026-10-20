/*
 * Copyright (C) 2026 ArcaneChat contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.thoughtcrime.securesms.R;

/**
 * Custom ActionMode callback shown when the user selects part of a message's text.
 *
 * <p>The platform ({@code Editor.TextActionModeCallback}) populates the menu with its default items
 * (Copy, Select all, Share, ...) <em>before</em> delegating to this callback, so {@link
 * #onCreateActionMode} only appends the app-specific Share entry and returns {@code true}. The
 * system items keep working through {@code TextView.onTextContextMenuItem()}: returning {@code
 * false} for them lets the platform perform the default action, so nothing is lost and no duplicate
 * menu appears.
 */
public class TextSelectionActionModeCallback implements ActionMode.Callback {

  private final Context context;
  private final TextView textView;

  public TextSelectionActionModeCallback(@NonNull Context context, @NonNull TextView textView) {
    this.context = context;
    this.textView = textView;
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    // The system menu items are already in place; just append our Share entry (once).
    // The platform also adds its own Share (android.R.id.shareText) when sharing is
    // supported — remove it so there is a single Share action and no duplicate menu.
    if (menu.findItem(R.id.menu_context_share_text) == null) {
      mode.getMenuInflater().inflate(R.menu.text_selection_context, menu);
    }
    menu.removeItem(android.R.id.shareText);
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    // Re-append after invalidations (e.g. Select all refreshes the menu); no duplicates.
    boolean changed = false;
    if (menu.findItem(R.id.menu_context_share_text) == null) {
      mode.getMenuInflater().inflate(R.menu.text_selection_context, menu);
      changed = true;
    }
    if (menu.findItem(android.R.id.shareText) != null) {
      menu.removeItem(android.R.id.shareText);
      changed = true;
    }
    return changed;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    if (item.getItemId() == R.id.menu_context_share_text) {
      shareText(getSelectedText());
      mode.finish();
      return true;
    }
    // System items (Copy, Select all, platform Share, ...) are handled by the TextView itself.
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {}

  private @NonNull String getSelectedText() {
    int start = textView.getSelectionStart();
    int end = textView.getSelectionEnd();
    if (start < 0 || end < 0 || start == end) {
      return "";
    }
    int len = textView.getText().length();
    start = Math.max(0, Math.min(start, len));
    end = Math.max(0, Math.min(end, len));
    return textView.getText().subSequence(Math.min(start, end), Math.max(start, end)).toString();
  }

  private void shareText(@NonNull String text) {
    if (text.isEmpty()) {
      return;
    }
    Intent sendIntent = new Intent(Intent.ACTION_SEND);
    sendIntent.setType("text/plain");
    sendIntent.putExtra(Intent.EXTRA_TEXT, text);
    Intent chooser = Intent.createChooser(sendIntent, context.getString(R.string.menu_share));
    if (!(context instanceof Activity)) {
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(chooser);
  }
}
