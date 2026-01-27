package org.thoughtcrime.securesms.linkpreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Represents metadata extracted from a URL for link preview display.
 */
public class LinkPreview implements Serializable {

    @NonNull
    private final String url;
    
    @Nullable
    private final String title;
    
    @Nullable
    private final String description;
    
    @Nullable
    private final String imageUrl;
    
    private final long timestamp;

    public LinkPreview(@NonNull String url, 
                      @Nullable String title, 
                      @Nullable String description,
                      @Nullable String imageUrl) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestamp = System.currentTimeMillis();
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean hasContent() {
        return (title != null && !title.trim().isEmpty()) || 
               (description != null && !description.trim().isEmpty()) ||
               (imageUrl != null && !imageUrl.trim().isEmpty());
    }
}
