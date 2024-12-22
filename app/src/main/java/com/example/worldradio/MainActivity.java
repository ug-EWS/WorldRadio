package com.example.worldradio;

import android.animation.TimeInterpolator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeTransform;
import androidx.transition.Explode;
import androidx.transition.Fade;
import androidx.transition.Scene;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.Visibility;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.RadioBrowser;

public class MainActivity extends AppCompatActivity {
    private LinearLayout layout;
    private LinearLayout list;
    private ImageView icon;
    TextView titleText;
    private FloatingActionButton addButton;
    private ImageView options;
    private ImageView settings;
    private RecyclerView listOfPlaylistsRecycler;
    private RecyclerView playlistRecycler;
    private PopupMenu settingsPopupMenu;
    private LinearLayout musicController;
    private TextView musicTitle;
    private ImageView replayButton;
    private ImageView playButton;
    private ImageView forwardButton;
    private ImageView musicIcon;
    private TextView noPlaylistsText;
    private TextView noVideosText;
    ExoPlayer exoPlayer;

    ListOfPlaylistsAdapter listOfPlaylistsAdapter;
    PlaylistAdapter playlistAdapter;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist, playingPlaylist;
    RadioStation playingRadioStation;

    int currentPlaylistIndex = -1,
            playingPlaylistIndex = -1,
            playingRadioStationIndex = -1,
            autoShutDown = 0;

    private PlaylistDialog playlistDialog;
    private RadioStationDialog radioStationDialog;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    RadioBrowser radioBrowser;

    boolean playlistOpen;
    boolean isPlaying;
    private boolean serviceRunning;

    private final Context context = MainActivity.this;
    Thread networkThread;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkThread = new Thread(() -> {
            String userAgent = "Demo agent/1.0";
            radioBrowser = new RadioBrowser(ConnectionParams.builder()
                    .apiUrl("https://de1.api.radio-browser.info/")
                    .userAgent(userAgent)
                    .timeout(5000)
                    .build());
        });
        networkThread.start();

        sp = getSharedPreferences("WorldRadio", MODE_PRIVATE);
        spe = sp.edit();

        autoShutDown = sp.getInt("autoShutDown", 0);

        listOfPlaylists = new ListOfPlaylists().fromJson(sp.getString("playlists", "[]"));

        playlistDialog = new PlaylistDialog(this,-1);
        radioStationDialog = new RadioStationDialog(this);

        initializeUi();
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (playlistOpen) setViewMode(false);
                else finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        exoPlayer = new ExoPlayer.Builder(this).build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkBatteryOptimizationSettings();
    }

    private void checkBatteryOptimizationSettings() {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            context.startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        serviceRunning = sp.getBoolean("serviceRunning", false);
        if (serviceRunning) {
            continuePlayback();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        refreshDatabase();
        if (playingRadioStationIndex != -1 && isPlaying) {
            exoPlayer.pause();
            startPlaybackService();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exoPlayer.release();
    }

    private void startPlaybackService() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        intent.setAction("com.opl.ACTION_START_SERVICE");
        intent.putExtra("playlist", playingPlaylist.getJson());
        spe.putInt("currentPlaylistIndex", playingPlaylistIndex)
                .putInt("currentVideoIndex", playingRadioStationIndex)
                .commit();
        startService(intent);
    }

    private void stopPlaybackService() {
        stopService(new Intent(MainActivity.this, PlaybackService.class));
    }

    private void continuePlayback() {
        stopPlaybackService();
        int cpi = sp.getInt("currentPlaylistIndex", 0);
        int cvi = sp.getInt("currentVideoIndex", 0);
        openPlaylist(cpi, cvi);
        playRadioStation(cvi, false);
        spe.putBoolean("serviceRunning", false);
    }

    private void initializeUi() {
        layout = findViewById(R.id.layoutMain);
        list = findViewById(R.id.list);
        icon = findViewById(R.id.icon);
        titleText = findViewById(R.id.titleText);
        addButton = findViewById(R.id.addButton);
        options = findViewById(R.id.options);
        settings = findViewById(R.id.settings);
        listOfPlaylistsRecycler = findViewById(R.id.listOfPlaylistsRecycler);
        listOfPlaylistsRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        playlistRecycler = findViewById(R.id.playlistRecycler);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        addButton.setOnClickListener(v -> {if (playlistOpen) radioStationDialog.show(); else playlistDialog.show();});
        icon.setOnClickListener(v -> setViewMode(false));
        options.setOnClickListener(v -> getPlaylistPopupMenu(options, true, currentPlaylistIndex).show());
        settings.setOnClickListener(v -> getSettingsPopupMenu().show());
        musicController = findViewById(R.id.musicController);
        musicTitle = findViewById(R.id.musicTitle);
        replayButton = findViewById(R.id.infoButton);
        replayButton.setOnClickListener(v -> controllerPrevious());
        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> controllerPlayPause());
        forwardButton = findViewById(R.id.addToButton);
        forwardButton.setOnClickListener(v -> controllerNext());
        musicIcon = findViewById(R.id.musicIcon);
        noPlaylistsText = findViewById(R.id.noPlaylists);
        noVideosText = findViewById(R.id.noVideos);

        setViewMode(false);
        setControllerVisibility(false);
    }

    void onStateChange(boolean _isPlaying) {
        if (_isPlaying != isPlaying) {
            isPlaying = _isPlaying;
            playButton.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
        }
    }

    @NonNull PopupMenu getPlaylistPopupMenu(View anchor, boolean current, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
        Playlist forPlaylist = listOfPlaylists.getPlaylistAt(index);
        menu.inflate(R.menu.playlist_options);
        menu.getMenu().getItem(0).setVisible(!current);
        menu.getMenu().getItem(1).setVisible(!current);
        menu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.addToTop) {
                playlistDialog.show(index);
                return true;
            }
            if (itemIndex == R.id.addToBottom) {
                playlistDialog.show(index + 1);
                return true;
            }
            if (itemIndex == R.id.edit) {
                new PlaylistDialog(this, index).show();
                return true;
            }
            if (itemIndex == R.id.delete) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this, R.style.Theme_OnlinePlaylistsDialogDark);
                b.setTitle(forPlaylist.title);
                b.setMessage(getString(R.string.delete_playlist_alert));
                b.setPositiveButton(getString(R.string.dialog_button_delete), ((dialog, which) -> {
                    listOfPlaylists.removePlaylist(index);
                    if (!current) {
                        listOfPlaylistsAdapter.removeItem(index);
                    }
                    dialog.dismiss();
                }));
                b.setNegativeButton(getString(R.string.dialog_button_no), ((dialog, which) -> dialog.dismiss()));
                b.create().show();
                return true;
            }
            return false;
        });
        return menu;
    }

    @NonNull PopupMenu getRadioStationPopupMenu(View anchor, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
        RadioStation forRadioStation = currentPlaylist.getRadioStationAt(index);
        menu.inflate(R.menu.video_options);
        menu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.addToTop) {
                radioStationDialog.show(index);
                return true;
            }
            if (itemIndex == R.id.addToBottom) {
                radioStationDialog.show(index + 1);
                return true;
            }
            if (itemIndex == R.id.addToPlaylist) {
                new ManagePlaylistsDialog(this, forRadioStation).show();
            }
            if (itemIndex == R.id.delete) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this, R.style.Theme_OnlinePlaylistsDialogDark);
                b.setTitle(forRadioStation.title);
                b.setMessage(getString(R.string.delete_video_alert));
                b.setPositiveButton(getString(R.string.dialog_button_delete), ((dialog, which) -> {
                    currentPlaylist.removeRadioStation(index);
                    playlistAdapter.removeItem(index);
                    dialog.dismiss();
                }));
                b.setNegativeButton(getString(R.string.dialog_button_no), ((dialog, which) -> dialog.dismiss()));
                b.create().show();
                return true;
            }
            if (itemIndex == R.id.info) {
                showInfo(forRadioStation);
                return true;
            }
            return false;
        });
        return menu;
    }

    private @NonNull PopupMenu getSettingsPopupMenu () {
        PopupMenu popupMenu = new PopupMenu(context, settings, Gravity.TOP);
        popupMenu.inflate(R.menu.options);
        Menu menu = popupMenu.getMenu();
        if (autoShutDown == 0) menu.findItem(R.id.autoDisabled).setChecked(true);
        if (autoShutDown == 1) menu.findItem(R.id.auto10Minutes).setChecked(true);
        if (autoShutDown == 2) menu.findItem(R.id.auto10Minutes).setChecked(true);
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemIndex = item.getItemId();
            if (itemIndex == R.id.autoDisabled) {
                autoShutDown = 0;
                item.setChecked(true);
                return true;
            }
            if (itemIndex == R.id.auto10Minutes) {
                autoShutDown = 1;
                item.setChecked(true);
                return true;
            }
            if (itemIndex == R.id.auto30Minutes) {
                autoShutDown = 2;
                item.setChecked(true);
                return true;
            }
            if (itemIndex == R.id.auto1Hour) {
                autoShutDown = 3;
                item.setChecked(true);
                return true;
            }
            if (itemIndex == R.id.about) {
                return true;
            }
            return true;
        });
        return popupMenu;
    }

    private void refreshDatabase() {
        spe.putString("playlists", listOfPlaylists.getJson())
                .putInt("autoShutDown", autoShutDown)
                .apply();
    }

    void openPlaylist(int index) {
        openPlaylist(index, 0);
    }

    void openPlaylist(int index, int scroll) {
        currentPlaylistIndex = index;
        currentPlaylist = listOfPlaylists.getPlaylistAt(index);
        setViewMode(true);
        playlistRecycler.scrollToPosition(scroll);
    }

    void playRadioStation(int index, boolean switchPlaylist) {
        if (!switchPlaylist && playingPlaylistIndex == currentPlaylistIndex) switchPlaylist = true;

        int oldPosition = playingRadioStationIndex;

        if (switchPlaylist) {
            playingPlaylistIndex = currentPlaylistIndex;
            playingPlaylist = currentPlaylist;
        }

        playingRadioStationIndex = index;
        playingRadioStation = currentPlaylist.getRadioStationAt(index);

        String favicon = playingRadioStation.faviconUrl;
        if (favicon.isEmpty()) musicIcon.setImageResource(R.drawable.baseline_radio_24);
        else Glide.with(this).load(favicon).into(musicIcon);
        musicTitle.setText(playingRadioStation.title);
        setControllerVisibility(true);

        if (switchPlaylist) {
            if (oldPosition != -1) playlistAdapter.notifyItemChanged(oldPosition);
            playlistAdapter.notifyItemChanged(index);
        }

        exoPlayerPlay();
        onStateChange(true);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void exoPlayerPlay() {
        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(playingRadioStation.url)).build();
        if (playingRadioStation.hls.equals("1")) {
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "exoplayer-codelab");
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            exoPlayer.setMediaSource(hlsMediaSource);
        } else {
            exoPlayer.setMediaItem(mediaItem);
        }
        exoPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void controllerPlayPause() {
        if (isPlaying) exoPlayer.pause();
        else exoPlayer.play();
        onStateChange(!isPlaying);
    }

    private void showInfo(RadioStation _station) {
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.Theme_OnlinePlaylistsDialogDark);
        b.setTitle(_station.title);
        CharSequence[] options = {
                getString(R.string.copy_stream_url),
                getString(R.string.copy_favicon_url),
                getString(R.string.copy_station_uuid)};
        b.setItems(options, (dialog, which) -> {
            if (which == 0) copyToClipboard(_station.url);
            if (which == 1) copyToClipboard(_station.faviconUrl);
            if (which == 2) copyToClipboard(_station.id);
            showMessage("Copied");
        });
        b.setPositiveButton(R.string.dialog_button_cancel, (dialog, which) -> {
            dialog.dismiss();
        });
        b.create().show();
    }

    private void controllerAdd() {
        new ManagePlaylistsDialog(this, playingRadioStation).show();
    }

    private void controllerPrevious() {
        int index = playingRadioStationIndex == 0 ? playingPlaylist.getLength() - 1 : playingRadioStationIndex - 1;
        playRadioStation(index, false);
    }

    private void controllerNext() {
        int index = playingRadioStationIndex == playingPlaylist.getLength() - 1 ? 0 : playingRadioStationIndex + 1;
        playRadioStation(index, false);
    }

    void closePlayer() {
        playingPlaylistIndex = -1;
        playingRadioStationIndex = -1;
        exoPlayer.pause();
        setControllerVisibility(false);
    }

    void showMessage(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    void copyToClipboard(String text) {
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", text));
    }

    private void setViewMode(boolean _openPlaylist) {
        playlistOpen = _openPlaylist;
        titleText.setText(playlistOpen ? currentPlaylist.title : getString(R.string.app_name));
        icon.setImageResource(playlistOpen ? R.drawable.baseline_arrow_back_24 : R.drawable.baseline_radio_24);
        icon.setClickable(playlistOpen);
        options.setVisibility(playlistOpen ? View.VISIBLE : View.GONE);

        listOfPlaylistsRecycler.setVisibility(View.GONE);
        noPlaylistsText.setVisibility(View.GONE);
        playlistRecycler.setVisibility(View.GONE);
        noVideosText.setVisibility(View.GONE);

        if (playlistOpen) {
            if (playlistAdapter == null) {
                playlistAdapter = new PlaylistAdapter(this);
                playlistRecycler.setAdapter(playlistAdapter);
                new ItemTouchHelper(new ItemMoveCallback(playlistAdapter)).attachToRecyclerView(playlistRecycler);
            } else {
                playlistAdapter.notifyDataSetChanged();
            }
            boolean noVideos = currentPlaylist.getLength() == 0;
            playlistRecycler.setVisibility(noVideos ? View.GONE : View.VISIBLE);
            noVideosText.setVisibility(noVideos ? View.VISIBLE : View.GONE);
        } else {
            if (listOfPlaylistsAdapter == null) {
                listOfPlaylistsAdapter = new ListOfPlaylistsAdapter(this);
                listOfPlaylistsRecycler.setAdapter(listOfPlaylistsAdapter);
                new ItemTouchHelper(new ItemMoveCallback(listOfPlaylistsAdapter)).attachToRecyclerView(listOfPlaylistsRecycler);
            } else {
                listOfPlaylistsAdapter.notifyDataSetChanged();
            }
            boolean noPlaylists = listOfPlaylists.getLength() == 0;
            listOfPlaylistsRecycler.setVisibility(noPlaylists ? View.GONE : View.VISIBLE);
            noPlaylistsText.setVisibility(noPlaylists ? View.VISIBLE : View.GONE);
        }
    }

    private void setControllerVisibility(boolean visible) {
        musicController.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

}