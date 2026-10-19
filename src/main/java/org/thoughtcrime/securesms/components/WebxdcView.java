package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.palette.graphics.Palette;
import com.b44t.messenger.DcMsg;
import java.io.ByteArrayInputStream;
import org.json.JSONObject;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.util.JsonUtils;

public class WebxdcView extends FrameLayout {

  private final @NonNull AppCompatImageView icon;
  private final @NonNull TextView appName;
  private final @NonNull TextView appSubtitle;
  private final @Nullable FrameLayout headerBg;

  private @Nullable SlideClickListener viewListener;

  public WebxdcView(@NonNull Context context) {
    this(context, null);
  }

  public WebxdcView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public WebxdcView(
      @NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    boolean compact;
    try (TypedArray a =
        context.getTheme().obtainStyledAttributes(attrs, R.styleable.WebxdcView, 0, 0)) {
      compact = a.getBoolean(R.styleable.WebxdcView_compact, false);
    }
    if (compact) {
      inflate(context, R.layout.webxdc_compact_view, this);
      this.headerBg = null;
    } else {
      inflate(context, R.layout.webxdc_view, this);
      this.headerBg = findViewById(R.id.webxdc_header_bg);
    }

    this.icon = findViewById(R.id.webxdc_icon);
    this.appName = findViewById(R.id.webxdc_app_name);
    this.appSubtitle = findViewById(R.id.webxdc_subtitle);
  }

  public void setWebxdcClickListener(@Nullable SlideClickListener listener) {
    this.viewListener = listener;
  }

  public void setWebxdc(final @NonNull DcMsg dcMsg, String defaultSummary) {
    JSONObject info = dcMsg.getWebxdcInfo();
    setOnClickListener(new OpenClickedListener(getContext(), dcMsg));

    // icon
    byte[] blob = dcMsg.getWebxdcBlob(JsonUtils.optString(info, "icon"));
    if (blob != null) {
      // Decode directly to Bitmap so we can extract colors
      Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
      if (bitmap != null) {
        icon.setImageBitmap(bitmap);
        if (headerBg != null) {
          applyGradient(headerBg, bitmap);
        }
      } else {
        ByteArrayInputStream is = new ByteArrayInputStream(blob);
        Drawable drawable = Drawable.createFromStream(is, "icon");
        icon.setImageDrawable(drawable);
      }
    }

    // name
    String docName = JsonUtils.optString(info, "document");
    String xdcName = JsonUtils.optString(info, "name");
    appName.setText(docName.isEmpty() ? xdcName : (docName + " – " + xdcName));

    // subtitle
    String summary = info.optString("summary");
    if (summary.isEmpty()) {
      summary = defaultSummary;
    }
    if (summary.isEmpty()) {
      appSubtitle.setVisibility(View.GONE);
    } else {
      appSubtitle.setVisibility(View.VISIBLE);
      appSubtitle.setText(summary);
    }
  }

  private void applyGradient(final FrameLayout backgroundView, final Bitmap bitmap) {
    Palette.from(bitmap)
        .generate(
            palette -> {
              int fallbackColor = getContext().getResources().getColor(R.color.def_primary);
              int brandColor = palette.getVibrantColor(palette.getDominantColor(fallbackColor));

              PaintDrawable drawable = new PaintDrawable();
              drawable.setShape(new RectShape());
              drawable.setShaderFactory(
                  new ShapeDrawable.ShaderFactory() {
                    @Override
                    public Shader resize(int width, int height) {
                      return new RadialGradient(
                          width / 2f,
                          height / 2f,
                          width / 1.5f,
                          brandColor,
                          Color.TRANSPARENT,
                          Shader.TileMode.CLAMP);
                    }
                  });

              backgroundView.setBackground(drawable);
            });
  }

  public String getDescription() {
    String type = getContext().getString(R.string.webxdc_app);
    String desc = type;
    desc += "\n" + appName.getText();
    if (appSubtitle.getText() != null
        && !appSubtitle.getText().toString().isEmpty()
        && !appSubtitle.getText().toString().equals(type)) {
      desc += "\n" + appSubtitle.getText();
    }
    return desc;
  }

  private class OpenClickedListener implements View.OnClickListener {
    private final @NonNull DocumentSlide slide;

    private OpenClickedListener(Context context, @NonNull DcMsg dcMsg) {
      this.slide = new DocumentSlide(context, dcMsg);
    }

    @Override
    public void onClick(View v) {
      if (viewListener != null) {
        viewListener.onClick(v, slide);
      }
    }
  }
}
