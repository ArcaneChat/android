# Link Previews Feature

## Overview

Link previews provide visual cards for URLs shared in messages, similar to Telegram and other modern messaging apps. When a user sends a message containing a URL, the app automatically fetches metadata (title, description, preview image) and displays it as a card below the message text.

## Features

- **Privacy-conscious**: Can be disabled in Privacy settings
- **Proxy support**: Respects proxy settings configured in the app
- **Caching**: Link previews are cached to avoid redundant fetches
- **Asynchronous loading**: Fetches happen in background threads to avoid blocking UI
- **Open Graph support**: Extracts Open Graph metadata (og:title, og:description, og:image)
- **Fallback metadata**: Falls back to HTML `<title>` and `<meta name="description">` if OG tags absent
- **Click to open**: Tapping the preview card opens the URL in a browser

## Architecture

### Components

1. **LinkPreview** (`linkpreview/LinkPreview.java`)
   - Data model representing link preview metadata
   - Contains URL, title, description, imageUrl, and timestamp

2. **LinkPreviewFetcher** (`linkpreview/LinkPreviewFetcher.java`)
   - Fetches link preview metadata from URLs
   - Respects proxy settings from DcContext
   - Extracts Open Graph and HTML metadata
   - Handles relative image URLs

3. **LinkPreviewUtil** (`linkpreview/LinkPreviewUtil.java`)
   - Utility methods for URL extraction
   - Pattern-based URL detection in message text

4. **LinkPreviewView** (`linkpreview/LinkPreviewView.java`)
   - Custom view for displaying link previews
   - Handles image loading via Glide
   - Opens URLs when tapped

5. **LinkPreviewCache** (`linkpreview/LinkPreviewCache.java`)
   - LRU cache for link previews (max 100 entries)
   - Avoids redundant network requests

### Integration

Link previews are integrated into `ConversationItem`:

- Added as a `ViewStub` in conversation item layouts
- Fetched asynchronously when message is bound
- Only shown for text messages with HTTP/HTTPS URLs
- Respects user's privacy preference setting

## Settings

**Privacy Setting**: Settings → Privacy → Link Previews

- Default: Enabled
- Key: `pref_link_previews`
- When disabled, no link previews are fetched or displayed

## User Experience

1. User sends/receives a message containing a URL
2. If link previews are enabled, app fetches metadata in background
3. Preview card appears below message text showing:
   - Preview image (if available)
   - Title
   - Description
   - Simplified domain name
4. Tapping the card opens the URL in default browser

## Privacy Considerations

As noted in the issue comments:

- **User control**: Link previews can be disabled entirely
- **Proxy respect**: All HTTP requests respect configured proxy settings
- **No tracking**: Preview fetches use generic User-Agent, no tracking headers
- **Caching**: Once fetched, previews are cached to minimize requests

## Technical Notes

### URL Extraction

- Uses regex pattern to detect HTTP/HTTPS URLs
- Extracts first URL from message text
- Ignores non-HTTP schemes

### Metadata Extraction

Priority order:
1. Open Graph tags (`og:title`, `og:description`, `og:image`)
2. HTML `<title>` tag
3. HTML `<meta name="description">` tag

### Limitations

- HTML content size limited to 500KB
- Only fetches from HTTP/HTTPS URLs
- Only displays preview for first URL in message
- No preview for other URL schemes (geo:, mailto:, etc.)

## Future Enhancements

Possible improvements:

- Multiple URL preview support
- Video preview support
- Audio preview support
- Preview editing/customization
- Preview size preferences
- Bandwidth-aware loading (WiFi only option)
