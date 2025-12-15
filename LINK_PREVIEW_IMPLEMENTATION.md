# Link Preview Implementation Summary

## Overview

This PR implements link preview functionality for the ArcaneChat Android app, allowing users to see preview cards for shared URLs similar to Telegram and other modern messengers.

## Changes Made

### Core Components

1. **LinkPreview.java** - Data model
   - Stores URL, title, description, imageUrl, and timestamp
   - Includes `hasContent()` method to check if preview has displayable data

2. **LinkPreviewFetcher.java** - Metadata fetcher
   - Fetches Open Graph and HTML metadata from URLs
   - Respects proxy settings from Delta Chat core (SOCKS5)
   - Handles HTTP/HTTPS connections with proper error handling
   - Parses og:title, og:description, og:image with HTML fallbacks
   - Makes relative image URLs absolute
   - HTML content size limited to 500KB

3. **LinkPreviewUtil.java** - URL extraction
   - Regex-based URL detection in message text
   - Extracts first HTTP/HTTPS URL from text
   - Improved regex to handle trailing punctuation

4. **LinkPreviewView.java** - UI component
   - Custom LinearLayout-based view
   - Displays title, description, image, and domain
   - Uses Glide for image loading
   - Clickable card opens URL in browser
   - Proper intent resolution checking

5. **LinkPreviewCache.java** - Caching system
   - Thread-safe LRU cache (100 entries)
   - Singleton with volatile instance field
   - Prevents redundant network requests

6. **LinkPreviewExecutor.java** - Thread management
   - Fixed thread pool (2 threads) for fetching
   - Singleton with volatile instance field
   - Prevents thread exhaustion

### UI Integration

1. **link_preview_view.xml** - Layout
   - MaterialCardView with proper styling
   - ImageView for preview image (120dp height)
   - TextViews for title, description, domain
   - Uses theme attributes for colors

2. **conversation_item_sent.xml & conversation_item_received.xml**
   - Added ViewStub for link preview
   - Proper margins and positioning

3. **ConversationItem.java** - Integration logic
   - Added `setLinkPreview()` method
   - Checks preference setting
   - Only shows for text messages with URLs
   - Async fetching with thread pool
   - Cache checking before fetch
   - UI updates on main thread

### Settings

1. **preferences_privacy.xml**
   - Added link preview toggle in Privacy section
   - Default: enabled

2. **Prefs.java**
   - Added `LINK_PREVIEWS` constant
   - Added `areLinkPreviewsEnabled()` method
   - Added `setLinkPreviewsEnabled()` method

3. **strings.xml**
   - Added "Link Previews" title
   - Added explanation text mentioning proxy respect
   - Added "Link preview image" content description

## Privacy & Security Considerations

✅ **User Control**: Can be disabled in Privacy settings
✅ **Proxy Support**: Respects SOCKS5 proxy configuration
✅ **No Tracking**: Generic User-Agent, no tracking headers
✅ **Size Limits**: 500KB HTML limit to prevent abuse
✅ **Timeout**: 10s connect, 10s read timeouts
✅ **Caching**: Minimizes network requests
✅ **Error Handling**: Graceful failures, no crashes

## Code Quality

All code review feedback addressed:
- ✅ Thread-safe singletons with volatile
- ✅ Thread pool instead of Thread creation
- ✅ Proper error handling (NumberFormatException, etc.)
- ✅ Intent resolution checking
- ✅ Correct size calculations
- ✅ Improved regex patterns
- ✅ Proper null checking
- ✅ Documentation comments

## Documentation

- `docs/LINK_PREVIEWS.md` - Full feature documentation
- Inline code comments
- This implementation summary

## Testing Recommendations

When build environment is available:

1. **Basic Functionality**
   - Send message with HTTP URL
   - Send message with HTTPS URL
   - Verify preview appears after fetching
   - Tap preview to open URL

2. **Edge Cases**
   - Message with multiple URLs (should show first)
   - URL with query parameters
   - URL with fragments
   - Non-English URLs
   - URLs without previews

3. **Settings**
   - Disable in Privacy settings
   - Verify no previews shown when disabled
   - Re-enable and verify they work again

4. **Proxy**
   - Configure SOCKS5 proxy
   - Send message with URL
   - Verify request goes through proxy

5. **Performance**
   - Send many messages with URLs quickly
   - Verify thread pool handles load
   - Check memory usage
   - Verify UI remains responsive

6. **Error Handling**
   - Invalid URLs
   - Timeout URLs
   - 404 URLs
   - Non-HTML content
   - Large HTML pages

## Known Limitations

1. **HTML Parsing**: Uses regex instead of proper HTML parser (Jsoup)
   - Trade-off: Simpler, no new dependency
   - May miss some edge cases with complex HTML
   
2. **Single URL**: Only shows preview for first URL in message
   - Could be extended to multiple previews in future

3. **No Video/Audio**: Only fetches static metadata
   - Could be extended to support video/audio previews

4. **No Size Preference**: Always loads previews
   - Could add WiFi-only option in future

## Migration Notes

No database changes required. Feature is additive only.

## Performance Impact

- Minimal: Async fetching, caching, thread pool
- Network: Only fetches when URL present and enabled
- Memory: LRU cache limited to 100 entries
- UI: No blocking, updates asynchronously

## Compatibility

- Min SDK: 21 (unchanged)
- Target SDK: 35 (unchanged)
- No new dependencies added
- Uses existing Glide for images
- Uses Material Design components already in app
