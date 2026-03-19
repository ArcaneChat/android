package org.webrtc;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import java.util.concurrent.CountDownLatch;

/**
 * Displays WebRTC video on a TextureView.
 *
 * <p>Unlike SurfaceViewRenderer (which extends SurfaceView and punches a transparent hole in the
 * window surface), TextureView is composited as part of the normal Android view hierarchy. This
 * means its contents are properly clipped by parent-view outlines, e.g. the rounded corners of a
 * CardView with {@code app:cardCornerRadius}.
 *
 * <p>API mirrors the commonly used subset of SurfaceViewRenderer so callers can swap the two with
 * minimal changes.
 */
public class TextureViewRenderer extends TextureView
    implements TextureView.SurfaceTextureListener, VideoSink, RendererCommon.RendererEvents {

  private final String resourceName;
  private final RendererCommon.VideoLayoutMeasure videoLayoutMeasure =
      new RendererCommon.VideoLayoutMeasure();
  private final EglRenderer eglRenderer;

  private volatile RendererCommon.RendererEvents rendererEvents;

  private final Object layoutLock = new Object();

  private int rotatedFrameWidth;
  private int rotatedFrameHeight;

  private boolean isFirstFrameRendered;

  public TextureViewRenderer(Context context) {
    super(context);
    resourceName = getResourceName();
    eglRenderer = new EglRenderer(resourceName);
    setSurfaceTextureListener(this);
  }

  public TextureViewRenderer(Context context, AttributeSet attrs) {
    super(context, attrs);
    resourceName = getResourceName();
    eglRenderer = new EglRenderer(resourceName);
    setSurfaceTextureListener(this);
  }

  /**
   * Initialize the renderer. Must be called on the main thread.
   *
   * @param sharedContext EGL context whose textures/framebuffers can be shared, or null.
   * @param rendererEvents listener for first-frame and resolution-change callbacks, or null.
   */
  public void init(
      EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
    ThreadUtils.checkIsOnMainThread();
    synchronized (layoutLock) {
      this.rendererEvents = rendererEvents;
      isFirstFrameRendered = false;
      rotatedFrameWidth = 0;
      rotatedFrameHeight = 0;
    }
    eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, new GlRectDrawer());
  }

  /** Block until any pending EGL work is done, then release all resources. */
  public void release() {
    eglRenderer.release();
  }

  public void setScalingType(RendererCommon.ScalingType scalingType) {
    ThreadUtils.checkIsOnMainThread();
    videoLayoutMeasure.setScalingType(scalingType);
    requestLayout();
  }

  public void setScalingType(
      RendererCommon.ScalingType scalingTypeMatchOrientation,
      RendererCommon.ScalingType scalingTypeMismatchOrientation) {
    ThreadUtils.checkIsOnMainThread();
    videoLayoutMeasure.setScalingType(
        scalingTypeMatchOrientation, scalingTypeMismatchOrientation);
    requestLayout();
  }

  /**
   * No-op. Hardware scaling is a SurfaceView-specific optimization and does not apply to
   * TextureView.
   */
  public void setEnableHardwareScaler(boolean enabled) {}

  public void setMirror(boolean mirror) {
    eglRenderer.setMirror(mirror);
  }

  public void setFpsReduction(float fps) {
    eglRenderer.setFpsReduction(fps);
  }

  public void disableFpsReduction() {
    eglRenderer.disableFpsReduction();
  }

  public void pauseVideo() {
    eglRenderer.pauseVideo();
  }

  public void addFrameListener(
      EglRenderer.FrameListener listener, float scale, RendererCommon.GlDrawer drawerParam) {
    eglRenderer.addFrameListener(listener, scale, drawerParam);
  }

  public void addFrameListener(EglRenderer.FrameListener listener, float scale) {
    eglRenderer.addFrameListener(listener, scale);
  }

  public void removeFrameListener(EglRenderer.FrameListener listener) {
    eglRenderer.removeFrameListener(listener);
  }

  public void clearImage() {
    eglRenderer.clearImage();
  }

  // ---- VideoSink ----

  @Override
  public void onFrame(VideoFrame frame) {
    updateFrameDimensions(frame);
    eglRenderer.onFrame(frame);
  }

  // ---- SurfaceTextureListener ----

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    eglRenderer.createEglSurface(surface);
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    final CountDownLatch completionLatch = new CountDownLatch(1);
    eglRenderer.releaseEglSurface(completionLatch::countDown);
    ThreadUtils.awaitUninterruptibly(completionLatch);
    return true;
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

  // ---- View measurement ----

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    ThreadUtils.checkIsOnMainThread();
    final Point size;
    synchronized (layoutLock) {
      size =
          videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight);
    }
    setMeasuredDimension(size.x, size.y);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    eglRenderer.setLayoutAspectRatio((right - left) / (float) (bottom - top));
  }

  // ---- RendererCommon.RendererEvents ----

  @Override
  public void onFirstFrameRendered() {
    if (rendererEvents != null) {
      rendererEvents.onFirstFrameRendered();
    }
  }

  @Override
  public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
    if (rendererEvents != null) {
      rendererEvents.onFrameResolutionChanged(videoWidth, videoHeight, rotation);
    }
  }

  // ---- Private helpers ----

  /**
   * Track frame dimensions and fire renderer-events callbacks. Called on the render thread from
   * onFrame(), so synchronize access to layout fields.
   */
  private void updateFrameDimensions(VideoFrame frame) {
    final boolean fireFirstFrame;
    final boolean fireSizeChange;
    final int newWidth;
    final int newHeight;
    final int rotation;

    synchronized (layoutLock) {
      fireFirstFrame = !isFirstFrameRendered;
      if (fireFirstFrame) {
        isFirstFrameRendered = true;
      }

      final int newRotatedWidth = frame.getRotatedWidth();
      final int newRotatedHeight = frame.getRotatedHeight();
      fireSizeChange =
          rotatedFrameWidth != newRotatedWidth || rotatedFrameHeight != newRotatedHeight;
      if (fireSizeChange) {
        rotatedFrameWidth = newRotatedWidth;
        rotatedFrameHeight = newRotatedHeight;
      }

      newWidth = frame.getBuffer().getWidth();
      newHeight = frame.getBuffer().getHeight();
      rotation = frame.getRotation();
    }

    if (fireFirstFrame) {
      post(this::onFirstFrameRendered);
    }
    if (fireSizeChange) {
      post(this::requestLayout);
      post(() -> onFrameResolutionChanged(newWidth, newHeight, rotation));
    }
  }

  private String getResourceName() {
    try {
      return getResources().getResourceEntryName(getId()) + ": ";
    } catch (android.content.res.Resources.NotFoundException e) {
      return "";
    }
  }
}
