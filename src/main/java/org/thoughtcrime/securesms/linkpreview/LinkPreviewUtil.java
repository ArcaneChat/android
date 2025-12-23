package org.thoughtcrime.securesms.linkpreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for link preview functionality.
 */
public class LinkPreviewUtil {

    // Pattern to match HTTP/HTTPS URLs in text
    // Matches URLs but uses lookahead to exclude trailing sentence punctuation
    // Note: This may occasionally exclude valid URLs ending with these characters
    // Trade-off chosen to improve common case where URLs are followed by punctuation
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[^\\s<>\"]+?(?=[\\s<>\"]|[.,;:!?']+(?:\\s|$)|$)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Extracts the first HTTP/HTTPS URL from the given text.
     */
    @Nullable
    public static String extractFirstUrl(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Extracts all HTTP/HTTPS URLs from the given text.
     */
    @NonNull
    public static List<String> extractAllUrls(@Nullable String text) {
        List<String> urls = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return urls;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group());
        }

        return urls;
    }

    /**
     * Checks if the given text contains at least one HTTP/HTTPS URL.
     */
    public static boolean containsUrl(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        return URL_PATTERN.matcher(text).find();
    }
}
