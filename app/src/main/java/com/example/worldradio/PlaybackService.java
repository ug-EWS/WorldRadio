package com.example.worldradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;

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
    private RemoteViews remoteViews;
    private MediaSession mediaSession;
    private Handler handler;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private ExoPlayer exoPlayer;

    private Playlist playlist;
    private int currentRadioStationIndex;
    private int autoShutDown;
    private int[] autoShutDownMillis = {0, 10000, 1800000, 3600000};
    private boolean isPlaying = false;
    private String title = "WorldRadio";


    private final String ACTION_START_SERVICE = "com.opl.ACTION_START_SERVICE";
    private final String ACTION_PLAY = "com.opl.ACTION_PLAY";
    private final String ACTION_CLOSE = "com.opl.ACTION_CLOSE";
    private final String ACTION_FORWARD = "com.opl.ACTION_FORWARD";
    private final String ACTION_PREV = "com.opl.ACTION_PREV";
    private final String ACTION_NEXT = "com.opl.ACTION_NEXT";
    private final String ACTION_CONTINUE = "com.opl.ACTION_CONTINUE";

    private final int SERVICE_NOTIFICATION = 101;
    private final int TIMEOUT_NOTIFICATION = 102;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("def", "Playback", NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
        mediaSession = new MediaSession(this, "WorldRadio");
        mediaSession.setMetadata(new MediaMetadata.Builder().putLong(MediaMetadata.METADATA_KEY_DURATION, -1L).build());
        sp = getSharedPreferences("WorldRadio", MODE_PRIVATE);
        spe = sp.edit();
        handler = new Handler(getMainLooper());
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(SERVICE_NOTIFICATION, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(SERVICE_NOTIFICATION, getNotification());
        }
    }

    private void startTimer(boolean delayed) {
        handler.postDelayed(() -> {
            notificationManager.notify(102, getTimeoutNotification());
            handler.postDelayed(this::stopSelf, 10000);
        }, delayed ? 10000 : autoShutDownMillis[autoShutDown]);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_START_SERVICE:
                        playlist = new Playlist().fromJson(intent.getStringExtra("playlist"));
                        currentRadioStationIndex = sp.getInt("currentVideoIndex", 0);
                        autoShutDown = sp.getInt("autoShutDown", 0);
                        spe.putBoolean("serviceRunning", true).commit();
                        initializePlayer();
                        if (autoShutDown != 0) startTimer(false);
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
                    case ACTION_CONTINUE:
                        handler.removeCallbacksAndMessages(null);
                        startTimer(true);
                        notificationManager.cancel(TIMEOUT_NOTIFICATION);
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
        notificationManager.cancel(TIMEOUT_NOTIFICATION);
        handler.removeCallbacksAndMessages(null);
    }

    private Notification getNotification() {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_radio_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(playlist.getRadioStationAt(currentRadioStationIndex).title)  // the label of the entry
                    .setContentText(playlist.title)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getPendingIntent())
                    .setDeleteIntent(getIntentFor(ACTION_CLOSE))
                    .setColor(getResources().getColor(R.color.very_dark_grey, getTheme()))
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
                    .setChannelId("def")
                    .setAutoCancel(false)
                    .setCustomContentView(getRemoteViews())
                    .build();
        } else {
            notification = new NotificationCompat.Builder(this, "def")
                    .setSmallIcon(R.drawable.baseline_radio_24)
                    .setTicker(getString(R.string.app_name))  // the status text
                    .setContentTitle(playlist.getRadioStationAt(currentRadioStationIndex).title)  // the label of the entry
                    .setContentText(playlist.title)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getPendingIntent())
                    .setDeleteIntent(getIntentFor(ACTION_CLOSE))
                    .setColor(getResources().getColor(R.color.very_dark_grey, getTheme()))
                    .setColorized(true)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .addAction(new NotificationCompat.Action.Builder(
                            R.drawable.baseline_skip_previous_24,
                            "Önceki",
                            getIntentFor(ACTION_PREV)).build())
                    .addAction(new NotificationCompat.Action.Builder(
                            isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24,
                            isPlaying ? "Duraklat" : "Çal",
                            getIntentFor(ACTION_PLAY)).build())
                    .addAction(new NotificationCompat.Action.Builder(
                            R.drawable.baseline_skip_next_24,
                            "Sonraki",
                            getIntentFor(ACTION_NEXT)).build())
                    .addAction(new NotificationCompat.Action.Builder(
                            R.drawable.baseline_fast_forward_24,
                            "İleri atla",
                            getIntentFor(ACTION_FORWARD)).build())
                    .addAction(new NotificationCompat.Action.Builder(
                            R.drawable.baseline_stop_24,
                            "Durdur",
                            getIntentFor(ACTION_CLOSE)).build())
                    .setChannelId("def")
                    .setAutoCancel(false)
                    .build();
        }
        return notification;
    }

    private @NonNull Notification getTimeoutNotification() {
        return new NotificationCompat.Builder(this, "def")
                .setSmallIcon(R.drawable.baseline_smart_display_24)
                .setTicker(getString(R.string.app_name))
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Oynatıcı otomatik olarak kapatılacak.")
                .setWhen(System.currentTimeMillis())
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true)
                .addAction(new NotificationCompat.Action.Builder(null, "Şimdi kapat", getIntentFor(ACTION_CLOSE)).build())
                .addAction(new NotificationCompat.Action.Builder(null, "30 dakika daha çal", getIntentFor(ACTION_CONTINUE)).build())
                .build();
    }

    private @NonNull RemoteViews getRemoteViews() {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.playback_notification);
        views.setImageViewResource(R.id.icon, R.drawable.baseline_radio_24);
        views.setTextViewText(R.id.title, title);
        return views;
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

    private void play(){
        if (isPlaying) exoPlayer.pause(); else exoPlayer.play();
        onStateChange(!isPlaying);
    }

    private void forward() {
        if (exoPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_DEFAULT_POSITION))
            exoPlayer.seekToDefaultPosition();
    }

    private void playPrevious() {
        currentRadioStationIndex = currentRadioStationIndex == 0 ? playlist.getLength() - 1 : currentRadioStationIndex - 1;
        changeRadioStation();
    }

    private void playNext() {
        currentRadioStationIndex = currentRadioStationIndex == playlist.getLength() - 1 ? 0 : currentRadioStationIndex + 1;
        changeRadioStation();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void changeRadioStation() {
        RadioStation station = playlist.getRadioStationAt(currentRadioStationIndex);

        if (exoPlayer != null) exoPlayer.release();
        exoPlayer = new ExoPlayer.Builder(this).build();

        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(station.url)).build();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "exoplayer-codelab");
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        exoPlayer.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true);
        if (station.hls.equals("1")) exoPlayer.setMediaSource(hlsMediaSource);
        else exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
        title = station.title;
        onStateChange(true);
        spe.putInt("currentVideoIndex", currentRadioStationIndex).commit();
    }
}
