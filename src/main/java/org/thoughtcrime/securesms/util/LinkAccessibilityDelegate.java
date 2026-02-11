package org.thoughtcrime.securesms.util;

import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * AccessibilityDelegate that exposes clickable links within a TextView to TalkBack.
 * Each link becomes a custom action that TalkBack users can activate.
 */
public class LinkAccessibilityDelegate extends View.AccessibilityDelegate {
  
  // Base ID for custom link actions (using a high number to avoid conflicts)
  private static final int LINK_ACTION_BASE_ID = 0x00FF0000;
  
  // Map to store span references for each action ID
  private final Map<Integer, LongClickCopySpan> actionSpanMap = new HashMap<>();

  @Override
  public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(host, info);
    
    if (!(host instanceof TextView)) {
      return;
    }
    
    TextView textView = (TextView) host;
    CharSequence text = textView.getText();
    
    if (!(text instanceof Spanned)) {
      return;
    }
    
    Spanned spanned = (Spanned) text;
    LongClickCopySpan[] spans = spanned.getSpans(0, spanned.length(), LongClickCopySpan.class);
    
    // Clear previous mappings
    actionSpanMap.clear();
    
    // Add a custom action for each link
    for (int i = 0; i < spans.length; i++) {
      final LongClickCopySpan span = spans[i];
      int start = spanned.getSpanStart(span);
      int end = spanned.getSpanEnd(span);
      
      if (start >= 0 && end > start && end <= spanned.length()) {
        String linkText = spanned.subSequence(start, end).toString();
        String label = "Open link: " + linkText;
        
        // Create a unique action ID for each link
        final int actionId = LINK_ACTION_BASE_ID + i;
        
        // Store the mapping
        actionSpanMap.put(actionId, span);
        
        AccessibilityNodeInfoCompat.AccessibilityActionCompat action =
            new AccessibilityNodeInfoCompat.AccessibilityActionCompat(actionId, label);
        
        AccessibilityNodeInfoCompat infoCompat = AccessibilityNodeInfoCompat.wrap(info);
        infoCompat.addAction(action);
      }
    }
  }
  
  @Override
  public boolean performAccessibilityAction(View host, int action, Bundle args) {
    // Check if this is one of our custom link actions
    if (actionSpanMap.containsKey(action)) {
      LongClickCopySpan span = actionSpanMap.get(action);
      if (span != null) {
        span.onClick(host);
        return true;
      }
    }
    
    return super.performAccessibilityAction(host, action, args);
  }
}
