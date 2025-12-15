package org.thoughtcrime.securesms.linkpreview;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;

/**
 * Custom view for displaying link previews.
 */
public class LinkPreviewView extends LinearLayout {

    private ImageView previewImage;
    private TextView titleText;
    private TextView descriptionText;
    private TextView urlText;
    private View cardView;

    private String currentUrl;

    public LinkPreviewView(Context context) {
        this(context, null);
    }

    public LinkPreviewView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinkPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.link_preview_view, this, true);
        
        cardView = findViewById(R.id.link_preview_card);
        previewImage = findViewById(R.id.link_preview_image);
        titleText = findViewById(R.id.link_preview_title);
        descriptionText = findViewById(R.id.link_preview_description);
        urlText = findViewById(R.id.link_preview_url);

        cardView.setOnClickListener(v -> {
            if (currentUrl != null) {
                openUrl(currentUrl);
            }
        });
    }

    /**
     * Binds a LinkPreview to this view.
     */
    public void bind(@Nullable LinkPreview preview, @NonNull GlideRequests glideRequests) {
        if (preview == null || !preview.hasContent()) {
            setVisibility(View.GONE);
            return;
        }

        setVisibility(View.VISIBLE);
        currentUrl = preview.getUrl();

        // Set title
        if (preview.getTitle() != null && !preview.getTitle().trim().isEmpty()) {
            titleText.setText(preview.getTitle());
            titleText.setVisibility(View.VISIBLE);
        } else {
            titleText.setVisibility(View.GONE);
        }

        // Set description
        if (preview.getDescription() != null && !preview.getDescription().trim().isEmpty()) {
            descriptionText.setText(preview.getDescription());
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        // Set URL
        urlText.setText(simplifyUrl(preview.getUrl()));

        // Load image
        if (preview.getImageUrl() != null && !preview.getImageUrl().trim().isEmpty()) {
            glideRequests
                .load(preview.getImageUrl())
                .centerCrop()
                .into(previewImage);
            previewImage.setVisibility(View.VISIBLE);
        } else {
            previewImage.setVisibility(View.GONE);
        }
    }

    /**
     * Simplifies a URL for display by extracting just the domain.
     */
    private String simplifyUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host != null) {
                // Remove www. prefix if present
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception e) {
            // Fall through to return original URL
        }
        return url;
    }

    /**
     * Opens the URL in a browser.
     */
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            getContext().startActivity(intent);
        } catch (Exception e) {
            // Handle error silently - user may not have a browser
        }
    }

    /**
     * Clears the preview and hides the view.
     */
    public void clear() {
        setVisibility(View.GONE);
        currentUrl = null;
        titleText.setText(null);
        descriptionText.setText(null);
        urlText.setText(null);
        previewImage.setImageDrawable(null);
    }
}
