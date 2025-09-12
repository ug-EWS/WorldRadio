package com.example.worldradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;

import java.util.Calendar;

public class PlaybackService extends Service {
    public class ServiceBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    private final IBinder binder = new ServiceBinder();

    private NotificationManager notificationManager;
    private MediaSession mediaSession;
    private MediaMetadata.Builder mediaMetadata;
    private PlaybackState.Builder playbackState;
    private Handler handler;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private ExoPlayer exoPlayer;

    private Playlist playlist;
    private int playingRadioStationIndex;
    private boolean timerSet;
    private long timerMs;
    private boolean isPlaying = false;


    private final String ACTION_START_SERVICE = "com.opl.ACTION_START_SERVICE";
    private final String ACTION_PLAY = "com.opl.ACTION_PLAY";
    private final String ACTION_CLOSE = "com.opl.ACTION_CLOSE";
    private final String ACTION_FORWARD = "com.opl.ACTION_FORWARD";
    private final String ACTION_PREV = "com.opl.ACTION_PREV";
    private final String ACTION_NEXT = "com.opl.ACTION_NEXT";

    private final int SERVICE_NOTIFICATION = 101;
    private final int TIMEOUT_NOTIFICATION = 102;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("def", "Playback", NotificationManager.IMPORTANCE_LOW);
        channel.enableVibration(false);
        channel.enableLights(false);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean _isPlaying) {
                isPlaying = _isPlaying;
                playbackState.setState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
                mediaSession.setPlaybackState(playbackState.build());
                startForegroundService();
                spe.putBoolean("playing", isPlaying).commit();
                Player.Listener.super.onIsPlayingChanged(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int _playbackState) {
                if (_playbackState == PlaybackState.STATE_BUFFERING) {
                    playbackState.setState(PlaybackState.STATE_BUFFERING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
                    mediaSession.setPlaybackState(playbackState.build());
                    startForegroundService();
                }
                Player.Listener.super.onPlaybackStateChanged(_playbackState);
            }
        });

        playbackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE
                        |PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        |PlaybackState.ACTION_SKIP_TO_NEXT
                        |PlaybackState.ACTION_STOP);

        mediaSession = new MediaSession(this, "WorldRadio");
        mediaSession.setPlaybackState(playbackState.build());
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                exoPlayer.play();
                onStateChange(true);
                super.onPlay();
            }

            @Override
            public void onPause() {
                exoPlayer.pause();
                onStateChange(false);
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                playNext();
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
                super.onSkipToPrevious();
            }

            @Override
            public void onStop() {
                super.onStop();
                stopSelf();
            }
        });

        mediaMetadata = new MediaMetadata.Builder();
        mediaMetadata = new MediaMetadata.Builder().putLong(MediaMetadata.METADATA_KEY_DURATION, -1L);
        mediaSession.setMetadata(mediaMetadata.build());

        sp = getSharedPreferences("WorldRadio", MODE_PRIVATE);
        spe = sp.edit();
        handler = new Handler(getMainLooper());
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SERVICE_NOTIFICATION, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(SERVICE_NOTIFICATION, getNotification());
        }
    }

    private void setTimer() {
        if (timerSet) {
            long afterMillis = timerMs - Calendar.getInstance().getTimeInMillis();
            if (afterMillis > 0) {
                handler = new Handler(getMainLooper());
                handler.postDelayed(() -> {
                    exoPlayer.pause();
                    timerSet = false;
                    spe.putBoolean("timerSet", false);
                }, afterMillis);
            } else {
                timerSet = false;
                spe.putBoolean("timerSet", false);
                setTimer();
            }
        } else if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_START_SERVICE:
                        playlist = new Playlist(intent.getStringExtra("playlist"), false);
                        playingRadioStationIndex = sp.getInt("playingVideoIndex", 0);
                        timerSet = sp.getBoolean("timerSet", false);
                        timerMs = sp.getLong("timerMs", 0);
                        spe.putBoolean("serviceRunning", true).commit();
                        initializePlayer();
                        setTimer();
                        break;
                    case ACTION_PLAY:
                        play();
                        break;
                    case ACTION_CLOSE:
                        notificationManager.cancel(TIMEOUT_NOTIFICATION);
                        stopSelf();
                        break;
                    case ACTION_FORWARD:
                        forward();
                        break;
                    case ACTION_PREV:
                        playPrevious();
                        break;
                    case ACTION_NEXT:
                        playNext();
                        break;
                    default:
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        exoPlayer.release();
        spe.putBoolean("serviceRunning", false).commit();
        notificationManager.cancel(SERVICE_NOTIFICATION);
        timerSet = false;
        setTimer();
    }

    private Notification getNotification() {
        Notification notification;

        Bitmap bitmap = null;
        try {
            bitmap = Glide.with(this).asBitmap().load(playlist.getRadioStationAt(playingRadioStationIndex).faviconUrl).submit().get();
            mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
            mediaSession.setMetadata(mediaMetadata.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification = new Notification.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_radio_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(playlist.getRadioStationAt(playingRadioStationIndex).title)  // the label of the entry
                    .setContentText(playlist.title)
                    .setShowWhen(false)
                    .setContentIntent(getPendingIntent())
                    .setDeleteIntent(getIntentFor(ACTION_CLOSE))
                    .setColorized(true)
                    .setStyle(new Notification.DecoratedMediaCustomViewStyle()
                            .setMediaSession(mediaSession.getSessionToken()))
                    .setAutoCancel(false)
                    .build();
        } else {
            notification = new Notification.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_radio_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(playlist.getRadioStationAt(playingRadioStationIndex).title)  // the label of the entry
                    .setContentText(playlist.title)
                    .setShowWhen(false)
                    .setContentIntent(getPendingIntent())
                    .setDeleteIntent(getIntentFor(ACTION_CLOSE))
                    .setColorized(true)
                    .setStyle(new Notification.DecoratedMediaCustomViewStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setMediaSession(mediaSession.getSessionToken()))
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_skip_previous_24),
                            "Önceki",
                            getIntentFor(ACTION_PREV)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24),
                            isPlaying ? "Duraklat" : "Çal",
                            getIntentFor(ACTION_PLAY)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_skip_next_24),
                            "Sonraki",
                            getIntentFor(ACTION_NEXT)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_fast_forward_24),
                            "İleri atla",
                            getIntentFor(ACTION_FORWARD)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.baseline_stop_24),
                            "Durdur",
                            getIntentFor(ACTION_CLOSE)).build())
                    .setAutoCancel(false)
                    .setLargeIcon(bitmap)
                    .build();
        }
        return notification;
    }

    private @NonNull PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private @NonNull PendingIntent getIntentFor(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private void initializePlayer() {
        changeRadioStation();
    }

    private void onStateChange(boolean _isPlaying) {
        isPlaying = _isPlaying;
        startForegroundService();
    }

    private void play() {
        if (isPlaying) exoPlayer.pause(); else exoPlayer.play();
    }

    private void forward() {
        if (exoPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_DEFAULT_POSITION))
            exoPlayer.seekToDefaultPosition();
    }

    private void playPrevious() {
        playingRadioStationIndex = playingRadioStationIndex == 0 ? playlist.getLength() - 1 : playingRadioStationIndex - 1;
        changeRadioStation();
    }

    private void playNext() {
        playingRadioStationIndex = playingRadioStationIndex == playlist.getLength() - 1 ? 0 : playingRadioStationIndex + 1;
        changeRadioStation();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void changeRadioStation() {
        RadioStation station = playlist.getRadioStationAt(playingRadioStationIndex);
        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(station.url)).build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
        spe.putInt("playingVideoIndex", playingRadioStationIndex).commit();
        startForegroundService();
    }
}