package org.thoughtcrime.securesms.util;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import java.util.regex.Pattern;

/* Utility for text linkify-ing */
public class Linkifier {
  static final int MAX_DISPLAY_LINK_LENGTH = 32;
  private static final String ELLIPSIS = "...";
  private static final Pattern CMD_PATTERN =
      Pattern.compile("(?<=^|\\s)/[a-zA-Z][a-zA-Z@\\d_/.-]{0,254}");
  private static final Pattern CUSTOM_PATTERN =
      Pattern.compile("(?<=^|\\s)(OPENPGP4FPR|openpgp4fpr|mumble|geo|gemini):[^ \\n]+");
  private static final Pattern PROXY_PATTERN =
      Pattern.compile("(?<=^|\\s)(SOCKS5|socks5|ss|SS):[^ \\n]+");
  private static final Pattern PHONE_PATTERN =
      Pattern.compile( // sdd = space, dot, or dash
          "(?<=^|\\s|\\.|\\()" // no letter at start
              + "(\\+[0-9]+[\\- \\.]*)?" // +<digits><sdd>*
              + "(\\([0-9]+\\)[\\- \\.]*)?" // (<digits>)<sdd>*
              + "([0-9][0-9\\- \\.]{3,}[0-9])" // <digit><digit|sdd>+<digit> (5 characters min)
              + "(?=$|\\s|\\.|\\))"); // no letter at end

  private static int brokenPhoneLinkifier = -1;

  private static boolean internalPhoneLinkifierNeeded() {
    if (brokenPhoneLinkifier == -1) { // unset
      if (Linkify.addLinks(new SpannableString("a100b"), Linkify.PHONE_NUMBERS)) {
        brokenPhoneLinkifier = 1; // true
      } else {
        brokenPhoneLinkifier = 0; // false
      }
    }
    return brokenPhoneLinkifier == 1;
  }

  private static void replaceURLSpan(SpannableStringBuilder messageBody, boolean shorten) {
    URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);
    // Iterate in reverse so that text replacements (messageBody.replace) do not shift the
    // positions of spans that haven't been processed yet.
    for (int i = urlSpans.length - 1; i >= 0; i--) {
      URLSpan urlSpan = urlSpans[i];
      int start = messageBody.getSpanStart(urlSpan);
      int end = messageBody.getSpanEnd(urlSpan);
      int spanEnd = end;

      if (shorten && start >= 0 && end > start) {
        String linkText = messageBody.subSequence(start, end).toString();
        String shortenedLinkText = shortenLink(linkText);

        if (!linkText.equals(shortenedLinkText)) {
          messageBody.replace(start, end, shortenedLinkText);
          spanEnd = start + shortenedLinkText.length();
        }
      }

      // LongClickCopySpan must not be derived from URLSpan, otherwise links will be removed on the
      // next addLinks() call
      messageBody.setSpan(
          new LongClickCopySpan(urlSpan.getURL()),
          start,
          spanEnd,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  static String shortenLink(String text) {
    if (text.length() <= MAX_DISPLAY_LINK_LENGTH) {
      return text;
    }

    // Keep the domain prefix (scheme + "://" + host + "/") and shorten only the path.
    int schemeEnd = text.indexOf("://");
    if (schemeEnd >= 0) {
      int slashAfterAuthority = text.indexOf('/', schemeEnd + 3);
      if (slashAfterAuthority >= 0) {
        String domainPart = text.substring(0, slashAfterAuthority + 1);
        String rest = text.substring(slashAfterAuthority + 1);
        int available = MAX_DISPLAY_LINK_LENGTH - domainPart.length();
        int tailLength = available - ELLIPSIS.length();
        if (tailLength > 0) {
          return domainPart + ELLIPSIS + rest.substring(rest.length() - Math.min(tailLength, rest.length()));
        } else {
          return domainPart + ELLIPSIS;
        }
      }
    }

    // Fallback for links without a "://host/path" structure: truncate at end.
    return text.substring(0, MAX_DISPLAY_LINK_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
  }

  public static SpannableStringBuilder linkify(SpannableStringBuilder messageBody) {
    // linkify commands such as `/echo` -
    // do this first to avoid `/xkcd_123456` to be treated partly as a phone number
    Linkify.addLinks(messageBody, CMD_PATTERN, "cmd:", null, null);
    replaceURLSpan(
        messageBody, false); // replace URLSpan so that it is not removed on the next addLinks() call

    Linkify.addLinks(messageBody, CUSTOM_PATTERN, null, null, null);
    replaceURLSpan(messageBody, false);

    if (Linkify.addLinks(messageBody, PROXY_PATTERN, null, null, null)) {
      replaceURLSpan(
          messageBody, false); // replace URLSpan so that it is not removed on the next addLinks() call
    }

    int flags;
    if (internalPhoneLinkifierNeeded()) {
      if (Linkify.addLinks(
          messageBody,
          PHONE_PATTERN,
          "tel:",
          Linkify.sPhoneNumberMatchFilter,
          Linkify.sPhoneNumberTransformFilter)) {
        replaceURLSpan(
            messageBody,
            false); // replace URLSpan so that it is not removed on the next addLinks() call
      }
      flags = Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS;
    } else {
      flags = Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS | Linkify.PHONE_NUMBERS;
    }

    // linkyfiy urls etc., this removes all existing URLSpan
    if (Linkify.addLinks(messageBody, flags)) {
      replaceURLSpan(messageBody, true);
    }

    return messageBody;
  }
}
