package org.thoughtcrime.securesms.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.WebxdcActivity;

/**
 * A {@link MediaSessionService} for webxdc mini-apps playing audio in a WebView.
 *
 * <p>The actual audio is played by the WebView's internal audio engine. This service holds a
 * {@link MediaSession} backed by a stub {@link SimpleBasePlayer} purely to post the system media
 * notification and respond to hardware media keys / notification play-pause buttons.
 *
 * <p>Communication with {@link WebxdcActivity} uses custom {@link SessionCommand}s:
 *
 * <ul>
 *   <li>{@code WEBXDC_AUDIO_STARTED} – audio began playing; args carry {@code title}, {@code
 *       artist}, {@code msg_id}, {@code account_id} so the notification tap reopens the correct
 *       webxdc instance.
 *   <li>{@code WEBXDC_AUDIO_STOPPED} – audio fully stopped; service removes the notification.
 *   <li>{@code WEBXDC_AUDIO_PAUSED} – audio paused by the app (not by the notification).
 *   <li>{@code WEBXDC_AUDIO_RESUMED} – audio resumed by the app (not by the notification).
 * </ul>
 *
 * When the user presses play/pause in the notification, the stub player's
 * {@link SimpleBasePlayer#handleSetPlayWhenReady} relays the command back to {@link
 * WebxdcActivity} via a broadcast so the WebView can pause/resume its audio/video elements.
 */
@OptIn(markerClass = UnstableApi.class)
public class WebxdcMediaSessionService extends MediaSessionService {

  public static final String COMMAND_AUDIO_STARTED = "WEBXDC_AUDIO_STARTED";
  public static final String COMMAND_AUDIO_STOPPED = "WEBXDC_AUDIO_STOPPED";
  public static final String COMMAND_AUDIO_PAUSED = "WEBXDC_AUDIO_PAUSED";
  public static final String COMMAND_AUDIO_RESUMED = "WEBXDC_AUDIO_RESUMED";

  /** Broadcast action sent when the system notification requests audio pause. */
  public static final String ACTION_NOTIFICATION_PAUSE =
      "org.thoughtcrime.securesms.WEBXDC_NOTIFICATION_PAUSE";

  /** Broadcast action sent when the system notification requests audio resume. */
  public static final String ACTION_NOTIFICATION_RESUME =
      "org.thoughtcrime.securesms.WEBXDC_NOTIFICATION_RESUME";

  private static final String TAG = WebxdcMediaSessionService.class.getSimpleName();

  private StubPlayer stubPlayer;
  private MediaSession session;

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  @Override
  public void onCreate() {
    super.onCreate();

    // Default session activity: open the conversation list. Updated by WEBXDC_AUDIO_STARTED.
    Intent defaultIntent =
        new Intent(this, org.thoughtcrime.securesms.ConversationListActivity.class);
    defaultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent defaultPendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            defaultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    stubPlayer = new StubPlayer();

    session =
        new MediaSession.Builder(this, stubPlayer)
            .setSessionActivity(defaultPendingIntent)
            .setCallback(new SessionCallbackImpl())
            .build();
  }

  @Nullable
  @Override
  public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
    return session;
  }

  @Override
  public void onDestroy() {
    if (session != null) {
      session.release();
      session = null;
    }
    if (stubPlayer != null) {
      stubPlayer.release();
      stubPlayer = null;
    }
    super.onDestroy();
  }

  // -------------------------------------------------------------------------
  // Stub player
  // -------------------------------------------------------------------------

  /**
   * Minimal {@link SimpleBasePlayer} that reports playback state to the {@link MediaSession}
   * without controlling any audio engine. Play/pause commands from the notification are relayed
   * back to {@link WebxdcActivity} via a broadcast.
   */
  @UnstableApi
  private final class StubPlayer extends SimpleBasePlayer {

    private boolean playWhenReady = false;
    private int playbackState = Player.STATE_IDLE;
    private ImmutableList<MediaItemData> playlist = ImmutableList.of();

    StubPlayer() {
      super(WebxdcMediaSessionService.this.getMainLooper());
    }

    @NonNull
    @Override
    protected State getState() {
      State.Builder builder =
          new State.Builder()
              .setAvailableCommands(
                  new Player.Commands.Builder()
                      .addAll(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP)
                      .build())
              .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
              .setPlaybackState(playbackState)
              .setPlaylist(playlist);
      if (!playlist.isEmpty()) {
        builder.setCurrentMediaItemIndex(0);
      }
      return builder.build();
    }

    /** Called when the user presses play/pause in the system notification. */
    @NonNull
    @Override
    protected ListenableFuture<?> handleSetPlayWhenReady(boolean play) {
      playWhenReady = play;
      Intent broadcast =
          new Intent(play ? ACTION_NOTIFICATION_RESUME : ACTION_NOTIFICATION_PAUSE);
      broadcast.setPackage(getPackageName());
      sendBroadcast(broadcast);
      invalidateState();
      return Futures.immediateFuture(null);
    }

    @NonNull
    @Override
    protected ListenableFuture<?> handleStop() {
      playWhenReady = false;
      playbackState = Player.STATE_IDLE;
      playlist = ImmutableList.of();
      Intent broadcast = new Intent(ACTION_NOTIFICATION_PAUSE);
      broadcast.setPackage(getPackageName());
      sendBroadcast(broadcast);
      invalidateState();
      return Futures.immediateFuture(null);
    }

    void setPlaying(String title, String artist) {
      playWhenReady = true;
      playbackState = Player.STATE_READY;
      playlist = buildPlaylist(title, artist);
      invalidateState();
    }

    void setPaused() {
      playWhenReady = false;
      playbackState = Player.STATE_READY;
      invalidateState();
    }

    void setStopped() {
      playWhenReady = false;
      playbackState = Player.STATE_IDLE;
      playlist = ImmutableList.of();
      invalidateState();
    }

    private ImmutableList<MediaItemData> buildPlaylist(String title, String artist) {
      androidx.media3.common.MediaMetadata metadata =
          new androidx.media3.common.MediaMetadata.Builder()
              .setTitle(title)
              .setArtist(artist)
              .build();
      androidx.media3.common.MediaItem mediaItem =
          new androidx.media3.common.MediaItem.Builder()
              .setMediaId("webxdc_audio")
              .setMediaMetadata(metadata)
              .build();
      return ImmutableList.of(
          new MediaItemData.Builder("webxdc_audio").setMediaItem(mediaItem).build());
    }
  }

  // -------------------------------------------------------------------------
  // Session callback
  // -------------------------------------------------------------------------

  private final class SessionCallbackImpl implements MediaSession.Callback {

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    @Override
    public MediaSession.ConnectionResult onConnect(
        @NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
      SessionCommands sessionCommands =
          MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
              .buildUpon()
              .add(new SessionCommand(COMMAND_AUDIO_STARTED, new Bundle()))
              .add(new SessionCommand(COMMAND_AUDIO_STOPPED, new Bundle()))
              .add(new SessionCommand(COMMAND_AUDIO_PAUSED, new Bundle()))
              .add(new SessionCommand(COMMAND_AUDIO_RESUMED, new Bundle()))
              .build();

      return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
          .setAvailableSessionCommands(sessionCommands)
          .build();
    }

    @NonNull
    @Override
    public ListenableFuture<SessionResult> onCustomCommand(
        @NonNull MediaSession session,
        @NonNull MediaSession.ControllerInfo controller,
        @NonNull SessionCommand customCommand,
        @NonNull Bundle args) {
      switch (customCommand.customAction) {
        case COMMAND_AUDIO_STARTED:
          handleAudioStarted(args);
          break;
        case COMMAND_AUDIO_STOPPED:
          if (stubPlayer != null) stubPlayer.setStopped();
          break;
        case COMMAND_AUDIO_PAUSED:
          if (stubPlayer != null) stubPlayer.setPaused();
          break;
        case COMMAND_AUDIO_RESUMED:
          if (stubPlayer != null)
            stubPlayer.setPlaying(args.getString("title", ""), args.getString("artist", ""));
          break;
        default:
          break;
      }
      return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
    }
  }

  // -------------------------------------------------------------------------
  // Command handlers
  // -------------------------------------------------------------------------

  private void handleAudioStarted(Bundle args) {
    String title = args.getString("title", "");
    String artist = args.getString("artist", "");

    if (args.containsKey("msg_id")) {
      int msgId = args.getInt("msg_id");
      int accountId = args.getInt("account_id", 0);
      updateSessionActivity(accountId, msgId);
    }
    if (stubPlayer != null) {
      stubPlayer.setPlaying(title, artist);
    }
    Log.i(TAG, "Audio started: title=" + title + " artist=" + artist);
  }

  @OptIn(markerClass = UnstableApi.class)
  private void updateSessionActivity(int accountId, int msgId) {
    try {
      Intent intent = new Intent(this, WebxdcActivity.class);
      intent.setAction(Intent.ACTION_VIEW);
      intent.putExtra("accountId", accountId);
      intent.putExtra("appMessageId", msgId);
      intent.putExtra("hideActionBar", false);
      intent.putExtra("href", "");
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent pendingIntent =
          PendingIntent.getActivity(
              this,
              0,
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      if (session != null) {
        session.setSessionActivity(pendingIntent);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to update session activity", e);
    }
  }
}
