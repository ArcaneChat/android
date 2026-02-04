package org.thoughtcrime.securesms.linkpreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.util.LRUCache;

import java.util.Collections;
import java.util.Map;

/**
 * Simple cache for link previews to avoid re-fetching.
 */
public class LinkPreviewCache {

    private static final int MAX_CACHE_SIZE = 100;
    
    private static volatile LinkPreviewCache instance;
    
    private final Map<String, LinkPreview> cache;

    private LinkPreviewCache() {
        cache = Collections.synchronizedMap(new LRUCache<String, LinkPreview>(MAX_CACHE_SIZE));
    }

    @NonNull
    public static LinkPreviewCache getInstance() {
        if (instance == null) {
            synchronized (LinkPreviewCache.class) {
                if (instance == null) {
                    instance = new LinkPreviewCache();
                }
            }
        }
        return instance;
    }

    public void put(@NonNull String url, @NonNull LinkPreview preview) {
        cache.put(url, preview);
    }

    @Nullable
    public LinkPreview get(@NonNull String url) {
        return cache.get(url);
    }

    public void clear() {
        cache.clear();
    }
}
