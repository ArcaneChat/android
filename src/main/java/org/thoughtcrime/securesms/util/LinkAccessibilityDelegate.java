package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.accessibility.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.thoughtcrime.securesms.R;

import java.util.HashMap;
import java.util.Map;

/**
 * AccessibilityDelegateCompat attached to the ConversationItem (parent view) so that TalkBack
 * can discover the links inside the child bodyText and expose them as custom actions.
 *
 * The bodyText TextView is marked importantForAccessibility="no" in the XML layout, which
 * means TalkBack focuses the parent ConversationItem instead.  The delegate therefore lives
 * on the parent but reads spans from the supplied bodyText reference.
 *
 * Using AccessibilityDelegateCompat (set via ViewCompat.setAccessibilityDelegate) instead of
 * the raw View.AccessibilityDelegate is important: the compat version's
 * onInitializeAccessibilityNodeInfo callback receives a properly-backed AccessibilityNodeInfoCompat
 * object, so addAction() reliably surfaces custom actions in the TalkBack context menu.
 */
public class LinkAccessibilityDelegate extends AccessibilityDelegateCompat {

  // Base ID for custom link actions. Using a high number (0x00FF0000, which is 16,711,680 in decimal)
  // to avoid conflicts with Android's standard accessibility action IDs, which are typically small integers
  // (e.g., ACTION_CLICK = 16, ACTION_LONG_CLICK = 32, etc.) or specific bit flags.
  // This range is safe for custom actions as per Android accessibility guidelines.
  private static final int LINK_ACTION_BASE_ID = 0x00FF0000;

  // Map to store span references for each action ID
  private final Map<Integer, LongClickCopySpan> actionSpanMap = new HashMap<>();

  private final Context context;
  // The child TextView whose Spanned text contains the LongClickCopySpan links.
  private TextView bodyText;

  public LinkAccessibilityDelegate(Context context, TextView bodyText) {
    this.context = context;
    this.bodyText = bodyText;
  }

  public void setBodyText(TextView bodyText) {
    this.bodyText = bodyText;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
    super.onInitializeAccessibilityNodeInfo(host, info);

    if (bodyText == null) {
      return;
    }

    CharSequence text = bodyText.getText();

    if (!(text instanceof Spanned)) {
      return;
    }

    Spanned spanned = (Spanned) text;
    LongClickCopySpan[] spans = spanned.getSpans(0, spanned.length(), LongClickCopySpan.class);

    // Clear and rebuild mappings each time. This is necessary because:
    // 1. TextView content may have changed since the last call
    // 2. The same delegate instance may be reused for different text content
    // 3. This method is called infrequently enough that performance is not a concern
    actionSpanMap.clear();

    // Add a custom action for each link
    for (int i = 0; i < spans.length; i++) {
      final LongClickCopySpan span = spans[i];
      int start = spanned.getSpanStart(span);
      int end = spanned.getSpanEnd(span);

      if (start >= 0 && end > start && end <= spanned.length()) {
        String linkText = spanned.subSequence(start, end).toString();
        String label = context.getString(R.string.accessibility_link_action, linkText);

        // Create a unique action ID for each link
        final int actionId = LINK_ACTION_BASE_ID + i;

        // Store the mapping
        actionSpanMap.put(actionId, span);

        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(actionId, label));
      }
    }
  }

  @Override
  public boolean performAccessibilityAction(View host, int action, Bundle args) {
    // Check if this is one of our custom link actions.
    // Pass bodyText as the widget so LongClickCopySpan.onClick() receives the correct view
    // (it calls widget.getContext() to reach the hosting Activity).
    if (actionSpanMap.containsKey(action)) {
      LongClickCopySpan span = actionSpanMap.get(action);
      if (span != null && bodyText != null) {
        span.onClick(bodyText);
        return true;
      }
    }

    return super.performAccessibilityAction(host, action, args);
  }
}
