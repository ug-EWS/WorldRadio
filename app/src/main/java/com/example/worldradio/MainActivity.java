package com.example.worldradio;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.app.UiModeManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class MainActivity extends AppCompatActivity implements RadioApi.LopCallback {
    private CoordinatorLayout coordinatorLayout;
    private LinearLayout list, toolbar, searchBar, startCancelTimerButton, mainScreen, layout;
    private ScrollView settingsScreen;
    private ImageView icon, options, replayButton, playButton, forwardButton, musicIcon,
            selectAllButton, removeButton, addToPlaylistButton, mergeButton,  cancelSearchButton, findUpButton, findDownButton, searchButton;
    private TextView musicTitle, warningText;
    private EditText searchEditText;
    private FloatingActionButton addButton;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private CardView controllerCard;
    RecyclerView listOfPlaylistsRecycler, playlistRecycler;
    TextView titleText;

    RadioApi radioApi;
    ExoPlayer exoPlayer;

    ListOfPlaylistsAdapter listOfPlaylistsAdapter;
    PlaylistAdapter playlistAdapter;

    ListOfPlaylists listOfPlaylists, countriesLop, languagesLop, tagsLop, topsLop, currentLop;
    Playlist currentPlaylist, playingPlaylist, sharedPlaylist;
    RadioStation playingRadioStation;

    int currentPlaylistIndex = -1,
            currentLopIndex = -1,
            playingLopIndex = -1,
            playingPlaylistIndex = -1,
            playingRadioStationIndex = -1,
            foundItemIndex = -1,
            foundAtStart = -1,
            foundAtEnd = -1,
            theme = 0;

    private long timerMs;

    private PlaylistDialog playlistDialog;
    private RadioStationDialog radioStationDialog;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    boolean playlistOpen, isPlaying, selectionMode, listSortMode, searchMode;
    private boolean serviceRunning, timerSet, settingsOpen, settingsInitialized;

    ArrayList<Integer> selectedItems;

    private final Context context = MainActivity.this;
    private Handler handler;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("WorldRadio", MODE_PRIVATE);
        spe = sp.edit();

        listOfPlaylists = new ListOfPlaylists().fromJson(sp.getString("playlists", "[]"));
        countriesLop = new ListOfPlaylists();
        languagesLop = new ListOfPlaylists();
        topsLop = new ListOfPlaylists();
        tagsLop = new ListOfPlaylists();

        theme = sp.getInt("theme", 0);

        playlistDialog = new PlaylistDialog(this,-1);
        radioStationDialog = new RadioStationDialog(this);

        selectedItems = new ArrayList<>();

        initializeUi();
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);

        radioApi = new RadioApi(this);
        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean _isPlaying) {
                isPlaying = _isPlaying;
                playButton.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                Player.Listener.super.onIsPlayingChanged(_isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                boolean isBuffering = playbackState == Player.STATE_BUFFERING;
                playButton.setVisibility(isBuffering ? View.GONE : View.VISIBLE);
                progressBar.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
                Player.Listener.super.onPlaybackStateChanged(playbackState);
            }
        });

        activityResultLauncher = getActivityResultLauncher();
    }

    private void back() {
        if (listSortMode) {
            setListSortMode(false);
        } else if (searchMode) {
            setSearchMode(false);
        } else if (selectionMode) {
            setSelectionMode(false);
        } else if (settingsOpen) {
            setSettingsOpen(false);
        } else if (playlistOpen) {
            setViewMode(false);
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW) && intent.getData() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                importPlaylist(getIntent().getData());
            } else {
                String[] permissions = {READ_EXTERNAL_STORAGE};
                requestPermissions(permissions, 101);
            }
        }

        if (sp.getBoolean("changing", false)) {
            int ppi = sp.getInt("ppi", -1);
            int pvi = sp.getInt("pvi", -1);
            int cpi = sp.getInt("cpi", -1);
            boolean play = sp.getBoolean("play", true);
            if (ppi != -1) openPlaylist(ppi);
            if (pvi != -1) playRadioStation(pvi, true, play);
            setViewMode(false);
            if (cpi != -1) openPlaylist(cpi);
            spe.putBoolean("changing", false);
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PERMISSION_DENIED) {
                showMessage(getString(R.string.grant_permission));
            } else {
                importPlaylist(getIntent().getData());
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private ActivityResultLauncher<Intent> getActivityResultLauncher() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                , (result) -> {
            if (result.getResultCode() == RESULT_OK) {
                Uri uri = result.getData().getData();
                OnlinePlaylistsUtils.writeFile(context, uri, sharedPlaylist.getJson());
                startActivity(Intent.createChooser(OnlinePlaylistsUtils.getShareIntent(uri), getString(R.string.share)));
            }
                });
    }

    @Override
    protected void onResume() {
        serviceRunning = sp.getBoolean("serviceRunning", false);
        if (serviceRunning) {
            continuePlayback();
            timerSet = sp.getBoolean("timerSet", false);
            timerMs = sp.getLong("timerMs", 0);
            setTimer();
        }
        super.onResume();

    }

    @Override
    protected void onPause() {
        refreshDatabase();
        if (!sp.getBoolean("changing", false)) {
            if (playingRadioStationIndex != -1 && isPlaying) {
                exoPlayer.pause();
                timerSet = false;
                setTimer();
                if (playingLopIndex == 0) startPlaybackService();
            }
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
        int cpi = sp.getInt("currentPlaylistIndex", -1);
        int cvi = sp.getInt("currentVideoIndex", -1);
        boolean play = sp.getBoolean("playing", true);
        openPlaylist(cpi, cvi);
        playRadioStation(cvi, true, play);
        spe.putBoolean("serviceRunning", false);
    }

    void startTimer(long millis) {
        timerSet = true;
        timerMs = Calendar.getInstance().getTimeInMillis() + millis;
        setTimer();
    }

    private void setTimer() {
        if (timerSet) {
            long afterMillis = timerMs - Calendar.getInstance().getTimeInMillis();
            if (afterMillis > 0) {
                handler = new Handler(getMainLooper());
                handler.postDelayed(() -> {
                    exoPlayer.pause();
                    timerSet = false;
                }, afterMillis);
            } else {
                timerSet = false;
                setTimer();
            }
        } else if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    private void initializeUi() {
        layout = findViewById(R.id.layoutMain);
        list = findViewById(R.id.list);
        mainScreen = findViewById(R.id.mainScreen);
        settingsScreen = findViewById(R.id.settingsScreen);
        icon = findViewById(R.id.icon);
        titleText = findViewById(R.id.titleText);
        addButton = findViewById(R.id.addButton);
        options = findViewById(R.id.options);
        selectAllButton = findViewById(R.id.selectAllButton);
        selectAllButton.setOnClickListener(v -> selectAllItems());
        removeButton = findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> removeItems());
        addToPlaylistButton = findViewById(R.id.addToPlaylistButton);
        addToPlaylistButton.setOnClickListener(v -> addItemsToPlaylist());
        mergeButton = findViewById(R.id.merge);
        mergeButton.setOnClickListener(v -> mergeItems());
        listOfPlaylistsRecycler = findViewById(R.id.listOfPlaylistsRecycler);
        listOfPlaylistsRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        playlistRecycler = findViewById(R.id.playlistRecycler);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        addButton.setOnClickListener(v -> {if (playlistOpen) radioStationDialog.show(); else playlistDialog.show();});
        icon.setOnClickListener(v -> back());
        options.setOnClickListener(v -> {
            if (!(playlistOpen || currentLopIndex == 0)) {
                setSettingsOpen(true);
            } else {
                (playlistOpen ? getPlaylistPopupMenu(options, true, currentPlaylistIndex) : getListOfPlaylistsPopupMenu()).show();
            }});
        musicTitle = findViewById(R.id.musicTitle);
        replayButton = findViewById(R.id.infoButton);
        replayButton.setOnClickListener(v -> controllerPrevious());
        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> controllerPlayPause());
        progressBar = findViewById(R.id.progressBar);
        forwardButton = findViewById(R.id.addToButton);
        forwardButton.setOnClickListener(v -> controllerNext());
        musicIcon = findViewById(R.id.musicIcon);
        warningText = findViewById(R.id.warning);
        selectAllButton.setTooltipText(getText(R.string.select_all));
        removeButton.setTooltipText(getText(R.string.delete));
        addToPlaylistButton.setTooltipText(getText(R.string.add_to_playlist));
        mergeButton.setTooltipText(getText(R.string.merge));

        toolbar = findViewById(R.id.toolbar);
        searchBar = findViewById(R.id.searchBar);
        cancelSearchButton = findViewById(R.id.cancelSearchButton);
        cancelSearchButton.setOnClickListener(v -> setSearchMode(false));
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                findItem(false, true);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        findUpButton = findViewById(R.id.findUpButton);
        findUpButton.setOnClickListener(v -> findItem(true, false));
        findDownButton = findViewById(R.id.findDownButton);
        findDownButton.setOnClickListener(v -> findItem(false, false));
        searchButton = findViewById(R.id.search);
        searchButton.setOnClickListener(v -> setSearchMode(true));

        coordinatorLayout = findViewById(R.id.main);

        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                openListOfPlaylists(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        controllerCard = findViewById(R.id.controllerCard);

        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Build.VERSION.SDK_INT >= 35 ? insets2.bottom : 0;
            v.setPadding(insets1.left, insets1.top, insets1.right, bottom);
            warningText.setPadding(0, 0, 0, insets1.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(addButton, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) addButton.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.rightMargin + insets1.bottom;
            addButton.setLayoutParams(layoutParams);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(settingsScreen, ((v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            settingsScreen.setPadding(0, 0, 0, insets1.bottom);
            return insets;
        }));

        ViewCompat.setOnApplyWindowInsetsListener(list, ((v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            list.setPadding(insets1.left, 0, insets1.right, 0);
            return insets;
        }));

        setSettingsOpen(false);
        setViewMode(false);
        setControllerVisibility(false);
        openListOfPlaylists(0);
    }

    @NonNull PopupMenu getListOfPlaylistsPopupMenu() {
        PopupMenu menu = new PopupMenu(context, options);
        menu.inflate(R.menu.list_of_playlists_options);
        menu.getMenu().getItem(0).setEnabled(!listOfPlaylists.isEmpty() && currentLopIndex == 0);
        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort) {
                setListSortMode(true);
                return true;
            }
            if (itemId == R.id.settings) {
                setSettingsOpen(true);
                return true;
            }
            return false;
        });
        return menu;
    }

    @NonNull PopupMenu getPlaylistPopupMenu(View anchor, boolean current, int index) {
        PopupMenu menu = new PopupMenu(context, anchor);
        menu.inflate(R.menu.playlist_options);
        Playlist forPlaylist = listOfPlaylists.getPlaylistAt(index);
        menu.getMenu().getItem(0).setVisible(!current);
        menu.getMenu().getItem(1).setVisible(!current);
        menu.getMenu().getItem(2).setVisible(current);
        menu.getMenu().getItem(6).setVisible(current);
        if (current) menu.getMenu().getItem(2).setEnabled(!currentPlaylist.isEmpty());
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
            if (itemIndex == R.id.sort) {
                setListSortMode(true);
                return true;
            }
            if (itemIndex == R.id.edit) {
                new PlaylistDialog(this, index).show();
                return true;
            }
            if (itemIndex == R.id.share) {
                sharePlaylist(forPlaylist);
                return true;
            }
            if (itemIndex == R.id.delete) {
                removePlaylist(index);
                return true;
            }
            if (itemIndex == R.id.settings) {
                setSettingsOpen(true);
                return true;
            }
            return false;
        });
        return menu;
    }

    void removePlaylist(int index) {
        OnlinePlaylistsUtils.showMessageDialog(
                context,
                listOfPlaylists.getPlaylistAt(index).title,
                R.string.delete_playlist_alert,
                R.string.dialog_button_delete,
                (dialog, which) -> {
                    listOfPlaylists.removePlaylist(index);
                    listOfPlaylistsAdapter.removeItem(index);
                    updateNoItemsView();
                },
                R.string.dialog_button_no,
                dialog -> listOfPlaylistsAdapter.notifyItemChanged(index)
                );
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
                new ManagePlaylistsDialog(this, index).show();
            }
            if (itemIndex == R.id.delete) {
                removeRadioStation(index);
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

    void removeRadioStation(int index) {
        OnlinePlaylistsUtils.showMessageDialog(
                context,
                currentPlaylist.getRadioStationAt(index).title,
                R.string.delete_video_alert,
                R.string.dialog_button_delete,
                (dialog, which) -> {
                    currentPlaylist.removeRadioStation(index);
                    playlistAdapter.removeItem(index);
                    updateNoItemsView();
                },
                R.string.dialog_button_no,
                dialog -> playlistAdapter.notifyItemChanged(index)
        );
    }

    private void initializeSettingsScreen() {
        RadioGroup themesButton = findViewById(R.id.themesButton);
        EditText autoShutDownEditText = findViewById(R.id.autoShutDownEditText);
        TextView timerStartText = findViewById(R.id.timerStartText);
        startCancelTimerButton = findViewById(R.id.startCancelTimerButton);
        List<Integer> themeButtons = List.of(R.id.light, R.id.dark, R.id.auto);


        themesButton.check(themeButtons.get(theme - 1 < 0 ? 2 : theme - 1));

        startCancelTimerButton.setOnClickListener(v -> {
            if (timerSet) {
                timerSet = false;
                setTimer();
                showMessage(R.string.timer_disabled);
            } else {
                String text = autoShutDownEditText.getText().toString();
                if (text.isEmpty()) text = autoShutDownEditText.getHint().toString();
                int minutes = Integer.parseInt(text);
                if (minutes > 0) startTimer(minutes * 60000L);
                showMessage(String.format(getString(R.string.timer_set), minutes));
            }
            autoShutDownEditText.setEnabled(!timerSet);
            timerStartText.setText(timerSet ? R.string.cancel_timer : R.string.start_timer);
        });

        themesButton.setOnCheckedChangeListener((group, checkedId) ->
                setAppTheme((themeButtons.indexOf(checkedId) + 1) % 3));

        settingsInitialized = true;
    }

    private void refreshDatabase() {
        spe.putBoolean("timerSet", timerSet)
                .putLong("timerMs", timerMs)
                .putInt("theme", theme)
                .commit();
        spe.putString("playlists", listOfPlaylists.getJson()).apply();
    }

    private void importPlaylist(Uri uri) {
        try {
            listOfPlaylists.addPlaylist(new Playlist().fromJson(OnlinePlaylistsUtils.readFile(this, uri)));
            showMessage(getString(R.string.import_success));
            listOfPlaylistsRecycler.scrollToPosition(0);
            listOfPlaylistsAdapter.notifyItemInserted(0);
            updateNoItemsView();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage(getString(R.string.import_fail));
        }
    }

    private void sharePlaylist(Playlist playlist) {
        sharedPlaylist = playlist;
        String fileName = playlist.title.replace("/", "_").concat(".json");
        activityResultLauncher.launch(OnlinePlaylistsUtils.getCreateIntent(fileName));
    }

    void openListOfPlaylists(int index) {
        currentLopIndex = index; // Custom, Top, Countries, Languages, Tags
        switch (currentLopIndex) {
            case 0: currentLop = listOfPlaylists; break;
            case 1: currentLop = topsLop; break;
            case 2: currentLop = countriesLop; break;
            case 3: currentLop = languagesLop; break;
            case 4: currentLop = tagsLop; break;
            default: break;
        }
        setViewMode(false);
        if (currentLopIndex != 0 && currentLop.isEmpty()) radioApi.getListOfPlaylists(currentLopIndex);
    }

    @Override
    public void onGotListOfPlaylists(ListOfPlaylists _lop, int _type) {
        runOnUiThread(() -> {
            switch (_type) {
                case 1: topsLop = _lop; currentLop = topsLop; break;
                case 2: countriesLop = _lop; currentLop = countriesLop; break;
                case 3: languagesLop = _lop; currentLop = languagesLop; break;
                case 4: tagsLop = _lop; currentLop = tagsLop; break;
                default: break;
            }
            setViewMode(playlistOpen);
            if (!playlistOpen) updateNoItemsView();
        });
    }

    @Override
    public void onGotRadioStations(Playlist radioStations) {
        runOnUiThread(() -> {
            currentPlaylist = radioStations;
            setViewMode(playlistOpen);
        });
    }

    @Override
    public String getStringById(int resId) {
        return getString(resId);
    }

    void openPlaylist(int index) {
        openPlaylist(index, index == playingPlaylistIndex ? playingRadioStationIndex : 0);
    }

    void openPlaylist(int index, int scroll) {
        currentPlaylistIndex = index;
        currentPlaylist = currentLop.getPlaylistAt(index);
        setViewMode(true);
        playlistRecycler.scrollToPosition(scroll);
        if (currentLopIndex != 0 && currentPlaylist.isEmpty()) radioApi.getRadioStations(currentPlaylist);
    }

    void playRadioStation(int index, boolean switchPlaylist, boolean autoPlay) {
        int oldPosition = playingRadioStationIndex;

        if (switchPlaylist) {
            playingLopIndex = currentLopIndex;
            playingPlaylistIndex = currentPlaylistIndex;
            playingPlaylist = currentPlaylist;
        }

        playingRadioStationIndex = index;
        playingRadioStation = playingPlaylist.getRadioStationAt(index);

        String favicon = playingRadioStation.faviconUrl;
        if (favicon.isEmpty()) musicIcon.setImageResource(R.drawable.baseline_radio_24);
        else Glide.with(this).load(favicon).into(musicIcon);
        musicTitle.setText(playingRadioStation.title);
        setControllerVisibility(true);

        if (currentPlaylistIndex == playingPlaylistIndex) {
            if (oldPosition != -1) playlistAdapter.notifyItemChanged(oldPosition);
            playlistAdapter.notifyItemChanged(index);
        }

        exoPlayerPlay(autoPlay);
    }

    private void exoPlayerPlay(boolean autoPlay) {
        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(playingRadioStation.url)).build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true);
        exoPlayer.prepare();
        if (OnlinePlaylistsUtils.isConnected(context)) {
            if (autoPlay) exoPlayer.play();
        }
        else showMessage(R.string.check_internet_connection);
    }

    private void controllerPlayPause() {
        if (isPlaying) exoPlayer.pause();
        else exoPlayer.play();
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
            showMessage(R.string.copied);
        });
        b.setPositiveButton(R.string.dialog_button_cancel, (dialog, which) -> dialog.dismiss());
        b.create().show();
    }

    private void controllerPrevious() {
        int index = playingRadioStationIndex == 0 ? playingPlaylist.getLength() - 1 : playingRadioStationIndex - 1;
        playRadioStation(index, false, true);
    }

    private void controllerNext() {
        int index = playingRadioStationIndex == playingPlaylist.getLength() - 1 ? 0 : playingRadioStationIndex + 1;
        playRadioStation(index, false, true);
    }

    void closePlayer() {
        playingPlaylistIndex = -1;
        playingRadioStationIndex = -1;
        exoPlayer.pause();
        setControllerVisibility(false);
    }

    void showMessage(int resId) {
        showMessage(getString(resId));
    }

    void showMessage(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    void copyToClipboard(String text) {
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", text));
    }

    private void setViewMode(boolean _openPlaylist) {
        if (_openPlaylist != playlistOpen) {
            playlistOpen = _openPlaylist;
            icon.setImageResource(playlistOpen ? R.drawable.baseline_arrow_back_24 : R.drawable.baseline_radio_24);
            icon.setClickable(playlistOpen);
        }

        if (playlistOpen) {
            if (playlistAdapter == null) {
                playlistAdapter = new PlaylistAdapter(this);
                playlistRecycler.setAdapter(playlistAdapter);
                new ItemTouchHelper(new ItemMoveCallback(playlistAdapter)).attachToRecyclerView(playlistRecycler);
            } else {
                playlistAdapter.notifyDataSetChanged();
            }
        } else {
            if (listOfPlaylistsAdapter == null) {
                listOfPlaylistsAdapter = new ListOfPlaylistsAdapter(this);
                listOfPlaylistsRecycler.setAdapter(listOfPlaylistsAdapter);
                new ItemTouchHelper(new ItemMoveCallback(listOfPlaylistsAdapter)).attachToRecyclerView(listOfPlaylistsRecycler);
            } else {
                listOfPlaylistsAdapter.notifyDataSetChanged();
            }
            currentPlaylistIndex = -1;
        }

        updateToolbar();
        updateNoItemsView();
    }

    private void setSettingsOpen(boolean _settingsOpen) {
        settingsOpen = _settingsOpen;
        TransitionManager.beginDelayedTransition(list, new MaterialSharedAxis(MaterialSharedAxis.X, settingsOpen));
        if (settingsOpen && !settingsInitialized) initializeSettingsScreen();
        mainScreen.setVisibility(settingsOpen ? View.GONE : View.VISIBLE);
        settingsScreen.setVisibility(settingsOpen ? View.VISIBLE : View.GONE);
        updateToolbar();
    }

    void setSelectionMode(boolean _selectionMode) {
        selectionMode = _selectionMode;
        (playlistOpen ? playlistAdapter : listOfPlaylistsAdapter).notifyDataSetChanged();
        updateToolbar();
    }

    void setListSortMode(boolean _listSortMode) {
        listSortMode = _listSortMode;
        (playlistOpen ? playlistAdapter : listOfPlaylistsAdapter).notifyDataSetChanged();
        updateToolbar();
    }

    void setSearchMode(boolean _searchMode) {
        searchMode = _searchMode;
        foundItemIndex = -1;
        (playlistOpen ? playlistAdapter : listOfPlaylistsAdapter).notifyDataSetChanged();
        updateToolbar();
        if (searchMode) OnlinePlaylistsUtils.showKeyboard(context, searchEditText);
        else OnlinePlaylistsUtils.hideKeyboard(context, searchEditText);
        searchEditText.setText("");
    }

    void updateToolbar() {
        boolean openAndEmpty = false;
        if (playlistOpen && currentPlaylist != null) if (currentPlaylist.isEmpty()) openAndEmpty = true;
        toolbar.setVisibility(searchMode ? View.GONE : View.VISIBLE);
        searchBar.setVisibility(searchMode ? View.VISIBLE : View.GONE);
        titleText.setText(
                listSortMode && playlistOpen ? getString(R.string.sort_videos)
                        : listSortMode ? getString(R.string.sort_playlists)
                        : selectionMode ? String.format(getString(R.string.multi_choose), selectedItems.size())
                        : settingsOpen ? getString(R.string.settings)
                        : playlistOpen ? currentPlaylist.title
                        : getString(R.string.app_name));
        icon.setImageResource(selectionMode ? R.drawable.baseline_close_24
                : playlistOpen || listSortMode || settingsOpen ? R.drawable.baseline_arrow_back_24
                : R.drawable.baseline_radio_24);
        icon.setClickable(playlistOpen || selectionMode || listSortMode || settingsOpen);
        if (icon.isClickable()) icon.clearColorFilter(); else icon.setColorFilter(getColor(R.color.teal_700));
        options.setVisibility(selectionMode || listSortMode || settingsOpen ? View.GONE : View.VISIBLE);
        selectAllButton.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        removeButton.setVisibility(selectionMode && currentLopIndex == 0 ? View.VISIBLE : View.GONE);
        addToPlaylistButton.setVisibility(selectionMode && playlistOpen ? View.VISIBLE : View.GONE);
        mergeButton.setVisibility(selectionMode && !playlistOpen && currentLopIndex == 0 ? View.VISIBLE : View.GONE);
        searchButton.setVisibility(selectionMode || listSortMode || openAndEmpty || settingsOpen ? View.GONE : View.VISIBLE);
        addButton.setVisibility(currentLopIndex != 0 || selectionMode || listSortMode || searchMode || settingsOpen ? View.GONE : View.VISIBLE);
        tabLayout.setVisibility(playlistOpen || searchMode || selectionMode || listSortMode || settingsOpen ? View.GONE : View.VISIBLE);
    }

    void updateNoItemsView() {
        listOfPlaylistsRecycler.setVisibility(View.GONE);
        playlistRecycler.setVisibility(View.GONE);
        warningText.setVisibility(View.GONE);
        String message = "";

        boolean showWarning = true;
        boolean isCurrentPlaylistNull = currentPlaylist == null;
        boolean isCurrentPlaylistEmpty = !isCurrentPlaylistNull && currentPlaylist.getLength() == 0;
        boolean isCurrentPlaylistLoading = !isCurrentPlaylistNull && currentPlaylist.isEmpty();
        boolean isCurrentLopNullOrEmpty = currentLop == null;
        isCurrentLopNullOrEmpty = !isCurrentLopNullOrEmpty && currentLop.isEmpty();

        if (playlistOpen) {
            if (isCurrentPlaylistEmpty) {
                message = getString(R.string.no_videos); // length = 0
            } else if (isCurrentPlaylistLoading) {
                message = getString(R.string.loading);
            } else {
                showWarning = false;
            }
        } else {
            if (isCurrentLopNullOrEmpty) {
                if (currentLopIndex == 0) {
                    message = getString(R.string.no_playlists);
                } else {
                    message = getString(R.string.loading);
                }
            } else {
                showWarning = false;
            }
        }

        if (showWarning) {
            warningText.setText(message);
            warningText.setVisibility(View.VISIBLE);
        } else {
            (playlistOpen ? playlistRecycler : listOfPlaylistsRecycler).setVisibility(View.VISIBLE);
        }
    }

    private void setControllerVisibility(boolean visible) {
        TransitionManager.beginDelayedTransition(layout);
        controllerCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void selectAllItems() {
        selectedItems.clear();
        int length;
        if (playlistOpen) length = currentPlaylist.getLength(); else length = listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) selectedItems.add(i);
        if (playlistOpen) playlistAdapter.notifyDataSetChanged();
        else listOfPlaylistsAdapter.notifyDataSetChanged();
        updateToolbar();
    }

    private void removeItems() {
        if (selectedItems.size() == 1) {
            int index = selectedItems.get(0);
            if (playlistOpen) removeRadioStation(index); else removePlaylist(index);
        } else OnlinePlaylistsUtils.showMessageDialog(
                context,
                R.string.multi_remove_title,
                String.format(getString(R.string.multi_remove_prompt), selectedItems.size()),
                R.string.dialog_button_delete,
                (dialog, which) -> {
                    if (playlistOpen) {
                        currentPlaylist.removeRadioStations(selectedItems);
                        if (selectedItems.contains(playingRadioStationIndex)) closePlayer();
                        if (currentPlaylistIndex == playingPlaylistIndex) playingRadioStationIndex = currentPlaylist.getIndexOf(playingRadioStation);
                    }
                    else {
                        listOfPlaylists.removePlaylists(selectedItems);
                        if (selectedItems.contains(playingPlaylistIndex)) closePlayer();
                        if (playingPlaylistIndex != -1) playingPlaylistIndex = listOfPlaylists.getIndexOf(playingPlaylist);
                    }
                    selectedItems.clear();
                    setSelectionMode(false);
                    updateNoItemsView();
                },
                R.string.dialog_button_no
        );
    }

    private void addItemsToPlaylist() {
        (selectedItems.size() == 1 ?
                new ManagePlaylistsDialog(this, selectedItems.get(0)):
                new ManagePlaylistsDialog(this, selectedItems))
                .show();
    }

    private void mergeItems() {
        if (selectedItems.size() > 1) {
            OnlinePlaylistsUtils.showMessageDialog(
                    context,
                    R.string.merge_title,
                    String.format(getString(R.string.merge_message), selectedItems.size()),
                    R.string.dialog_button_merge,
                    (dialog, which) -> {
                        String s = listOfPlaylists.mergePlaylists(selectedItems);
                        selectedItems.clear();
                        setSelectionMode(false);
                        showMessage(String.format(getString(R.string.merge_success), s));
                    },
                    R.string.dialog_button_no);
        } else showMessage(R.string.choose_more_than_one);
    }

    private void findItem(boolean up, boolean _new) {
        int f = foundItemIndex;
        int i = _new ? -1 : foundItemIndex;
        String query = searchEditText.getText().toString().toLowerCase();
        Function<Integer, String> title =
                playlistOpen ? (index) -> currentPlaylist.getRadioStationAt(index).title
                        : (index) -> currentLop.getPlaylistAt(index).title;
        Supplier<Integer> length =
                playlistOpen ? () -> currentPlaylist.getLength() : () -> currentLop.getLength();
        if (!query.isEmpty()) {
            while (up && i > 0 || !up && i < length.get() - 1) {
                if (up) i--;
                else i++;
                String title1 = title.apply(i).toLowerCase();
                if (title1.contains(query)) {
                    foundItemIndex = i;
                    foundAtStart = title1.indexOf(query);
                    foundAtEnd = foundAtStart + query.length();
                    break;
                }
            }
            (playlistOpen ? playlistAdapter : listOfPlaylistsAdapter).notifyItemChanged(f);
            (playlistOpen ? playlistAdapter : listOfPlaylistsAdapter).notifyItemChanged(foundItemIndex);
            (playlistOpen ? playlistRecycler : listOfPlaylistsRecycler).scrollToPosition(foundItemIndex);
        }
    }

    private void setAppTheme(int _theme) {
        theme = _theme;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ((UiModeManager) getSystemService(UI_MODE_SERVICE)).setApplicationNightMode(theme);
        else AppCompatDelegate.setDefaultNightMode(theme == 0 ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : theme);
        if (playingLopIndex == 0)
            spe.putBoolean("changing", true)
                .putInt("ppi", playingPlaylistIndex)
                .putInt("pvi", playingRadioStationIndex)
                .putInt("cpi", currentPlaylistIndex)
                .putBoolean("play", isPlaying);
    }
}