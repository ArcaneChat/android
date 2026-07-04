package org.thoughtcrime.securesms.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

/* Utility for text linkify-ing */
public class Linkifier {
  static final int MAX_DISPLAY_LINK_LENGTH = 32;
  private static final String MIDDLE_ELLIPSIS = "...";
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

  private static void replaceURLSpan(Spannable messageBody) {
    URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);
    for (URLSpan urlSpan : urlSpans) {
      int start = messageBody.getSpanStart(urlSpan);
      int end = messageBody.getSpanEnd(urlSpan);
      // LongClickCopySpan must not be derived from URLSpan, otherwise links will be removed on the
      // next addLinks() call
      messageBody.setSpan(
          new LongClickCopySpan(urlSpan.getURL()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  private static void shortenLongLinks(Spannable messageBody) {
    LongClickCopySpan[] urlSpans =
        messageBody.getSpans(0, messageBody.length(), LongClickCopySpan.class);

    // Process spans from end to start so text replacement does not invalidate earlier positions.
    Arrays.sort(urlSpans, Comparator.comparingInt(messageBody::getSpanStart).reversed());

    for (LongClickCopySpan urlSpan : urlSpans) {
      int start = messageBody.getSpanStart(urlSpan);
      int end = messageBody.getSpanEnd(urlSpan);

      if (start < 0 || end <= start) {
        continue;
      }

      String linkText = messageBody.subSequence(start, end).toString();
      String shortenedLinkText = shortenMiddle(linkText);

      if (linkText.equals(shortenedLinkText)) {
        continue;
      }

      messageBody.removeSpan(urlSpan);
      messageBody.replace(start, end, shortenedLinkText);
      messageBody.setSpan(
          urlSpan, start, start + shortenedLinkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  static String shortenMiddle(String text) {
    if (text.length() <= MAX_DISPLAY_LINK_LENGTH) {
      return text;
    }

    int visibleCharacters = MAX_DISPLAY_LINK_LENGTH - MIDDLE_ELLIPSIS.length();
    int prefixLength = (visibleCharacters + 1) / 2;
    int suffixLength = visibleCharacters - prefixLength;

    return text.substring(0, prefixLength)
        + MIDDLE_ELLIPSIS
        + text.substring(text.length() - suffixLength);
  }

  public static Spannable linkify(Spannable messageBody) {
    // linkify commands such as `/echo` -
    // do this first to avoid `/xkcd_123456` to be treated partly as a phone number
    Linkify.addLinks(messageBody, CMD_PATTERN, "cmd:", null, null);
    replaceURLSpan(
        messageBody); // replace URLSpan so that it is not removed on the next addLinks() call

    Linkify.addLinks(messageBody, CUSTOM_PATTERN, null, null, null);
    replaceURLSpan(messageBody);

    if (Linkify.addLinks(messageBody, PROXY_PATTERN, null, null, null)) {
      replaceURLSpan(
          messageBody); // replace URLSpan so that it is not removed on the next addLinks() call
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
            messageBody); // replace URLSpan so that it is not removed on the next addLinks() call
      }
      flags = Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS;
    } else {
      flags = Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS | Linkify.PHONE_NUMBERS;
    }

    // linkyfiy urls etc., this removes all existing URLSpan
    if (Linkify.addLinks(messageBody, flags)) {
      replaceURLSpan(messageBody);
    }

    shortenLongLinks(messageBody);

    return messageBody;
  }
}
