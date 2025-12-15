package org.thoughtcrime.securesms.linkpreview;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches link preview metadata from URLs.
 * Respects proxy settings configured in Delta Chat.
 */
public class LinkPreviewFetcher {

    private static final String TAG = LinkPreviewFetcher.class.getSimpleName();
    
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_HTML_SIZE = 500000; // 500KB limit
    
    // Patterns for extracting Open Graph and basic HTML metadata
    // Note: These patterns are simplified and may not handle all HTML variations.
    // For production use with complex sites, consider using a proper HTML parser like Jsoup.
    private static final Pattern OG_TITLE_PATTERN = 
        Pattern.compile("<meta[^>]*property=['\"]og:title['\"][^>]*content=['\"]([^'\"]*)['\"][^>]*>", 
                       Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESCRIPTION_PATTERN = 
        Pattern.compile("<meta[^>]*property=['\"]og:description['\"][^>]*content=['\"]([^'\"]*)['\"][^>]*>", 
                       Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_PATTERN = 
        Pattern.compile("<meta[^>]*property=['\"]og:image['\"][^>]*content=['\"]([^'\"]*)['\"][^>]*>", 
                       Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = 
        Pattern.compile("<title[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESCRIPTION_PATTERN = 
        Pattern.compile("<meta[^>]*name=['\"]description['\"][^>]*content=['\"]([^'\"]*)['\"][^>]*>", 
                       Pattern.CASE_INSENSITIVE);

    private final Context context;

    public LinkPreviewFetcher(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Fetches link preview metadata from the given URL.
     * This is a blocking operation and should be called on a background thread.
     */
    @WorkerThread
    @Nullable
    public LinkPreview fetchPreview(@NonNull String urlString) {
        try {
            URL url = new URL(urlString);
            
            // Only fetch from http/https URLs
            if (!url.getProtocol().equalsIgnoreCase("http") && 
                !url.getProtocol().equalsIgnoreCase("https")) {
                return null;
            }

            HttpURLConnection connection = openConnection(url);
            if (connection == null) {
                return null;
            }

            try {
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Failed to fetch preview, response code: " + responseCode);
                    return null;
                }

                String contentType = connection.getContentType();
                if (contentType == null || !contentType.toLowerCase().contains("text/html")) {
                    Log.d(TAG, "Skipping non-HTML content: " + contentType);
                    return null;
                }

                String html = readHtml(connection);
                if (html == null) {
                    return null;
                }

                return extractMetadata(urlString, html);
                
            } finally {
                connection.disconnect();
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch link preview", e);
            return null;
        }
    }

    @Nullable
    private HttpURLConnection openConnection(@NonNull URL url) {
        try {
            DcContext dcContext = DcHelper.getContext(context);
            String proxyConfig = dcContext.getConfig("socks5_host");
            
            HttpURLConnection connection;
            
            if (!TextUtils.isEmpty(proxyConfig)) {
                // Parse proxy configuration: host:port
                String[] parts = proxyConfig.split(":");
                if (parts.length >= 2) {
                    try {
                        String host = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        
                        Proxy proxy = new Proxy(Proxy.Type.SOCKS, 
                                               new InetSocketAddress(host, port));
                        connection = (HttpURLConnection) url.openConnection(proxy);
                        Log.d(TAG, "Using SOCKS5 proxy: " + host + ":" + port);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid proxy port, using direct connection", e);
                        connection = (HttpURLConnection) url.openConnection();
                    }
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                }
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            
            return connection;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to open connection", e);
            return null;
        }
    }

    @Nullable
    private String readHtml(@NonNull HttpURLConnection connection) throws IOException {
        StringBuilder html = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            
            String line;
            int totalSize = 0;
            
            while ((line = reader.readLine()) != null) {
                totalSize += line.length();
                if (totalSize > MAX_HTML_SIZE) {
                    Log.w(TAG, "HTML size exceeds limit, stopping read");
                    break;
                }
                html.append(line).append("\n");
                
                // Early exit if we have all the metadata we need (optimization)
                if (hasAllMetadata(html.toString())) {
                    break;
                }
            }
        }
        
        return html.toString();
    }

    private boolean hasAllMetadata(String html) {
        // Check if we have found all Open Graph tags
        return OG_TITLE_PATTERN.matcher(html).find() &&
               OG_DESCRIPTION_PATTERN.matcher(html).find() &&
               OG_IMAGE_PATTERN.matcher(html).find();
    }

    @Nullable
    private LinkPreview extractMetadata(@NonNull String url, @NonNull String html) {
        String title = extractPattern(OG_TITLE_PATTERN, html);
        if (title == null) {
            title = extractPattern(TITLE_PATTERN, html);
        }
        
        String description = extractPattern(OG_DESCRIPTION_PATTERN, html);
        if (description == null) {
            description = extractPattern(META_DESCRIPTION_PATTERN, html);
        }
        
        String imageUrl = extractPattern(OG_IMAGE_PATTERN, html);
        
        // Make image URL absolute if it's relative
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            try {
                URL baseUrl = new URL(url);
                if (imageUrl.startsWith("//")) {
                    imageUrl = baseUrl.getProtocol() + ":" + imageUrl;
                } else if (imageUrl.startsWith("/")) {
                    imageUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + imageUrl;
                } else {
                    String path = baseUrl.getPath();
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        path = path.substring(0, lastSlash + 1);
                    }
                    imageUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + path + imageUrl;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to make image URL absolute", e);
                imageUrl = null;
            }
        }
        
        LinkPreview preview = new LinkPreview(url, 
                                             cleanHtmlEntities(title), 
                                             cleanHtmlEntities(description), 
                                             imageUrl);
        
        return preview.hasContent() ? preview : null;
    }

    @Nullable
    private String extractPattern(@NonNull Pattern pattern, @NonNull String html) {
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            String value = matcher.group(1);
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        }
        return null;
    }

    @Nullable
    private String cleanHtmlEntities(@Nullable String text) {
        if (text == null) return null;
        
        return text.replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&#39;", "'")
                  .replace("&nbsp;", " ")
                  .trim();
    }
}
