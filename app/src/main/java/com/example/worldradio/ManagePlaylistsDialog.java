package com.example.worldradio;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class ManagePlaylistsDialog {
    MainActivity activity;

    AlertDialog.Builder builder;
    AlertDialog dialog;
    RecyclerView recyclerView;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    RadioStation station;

    ArrayList<CharSequence> items;
    ArrayList<Integer> originalIndexes;
    ArrayList<Integer> forRadioStations;
    boolean[] contains;
    boolean custom;
    int length;

    ManagePlaylistsDialog(MainActivity _activity, int _forRadioStation) {
        initializeDialog(_activity);
        station = currentPlaylist.getRadioStationAt(_forRadioStation);

        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(i);
            if (!playlist.contains(station)) {
                items.add(playlist.title);
                originalIndexes.add(i);
            }
        }

        length = items.size();
        contains = new boolean[length];
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

        builder.setPositiveButton(custom ? R.string.copy : R.string.dialog_button_add, (dialog1, which) -> {
            copyRadioStation();
            activity.showMessage(R.string.copied);
        });

        if (custom) {
            builder.setNeutralButton(R.string.move, (dialog1, which) -> {
                copyRadioStation();
                currentPlaylist.removeRadioStation(_forRadioStation);
                activity.playlistAdapter.removeItem(_forRadioStation);
                activity.showMessage(R.string.moved);
            });
        }
        dialog = builder.create();
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forRadioStations) {
        initializeDialog(_activity);

        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();
        forRadioStations = _forRadioStations;

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            if (activity.currentLopIndex != 0 || activity.currentPlaylistIndex != i) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(i);
                items.add(playlist.title);
                originalIndexes.add(i);
            }
        }

        length = items.size();
        contains = new boolean[length];
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

        builder.setPositiveButton(custom ? R.string.copy : R.string.dialog_button_add, (dialog1, which) -> {
            copyRadioStations();
            activity.showMessage(custom ? activity.getString(R.string.copied) : "Eklendi.");
            activity.setSelectionMode(false);
        });

        if (custom) {
            builder.setNeutralButton(R.string.move, (dialog1, which) -> {
                copyRadioStations();
                currentPlaylist.removeRadioStations(forRadioStations);
                activity.showMessage(R.string.moved);
                activity.setSelectionMode(false);
            });
        }
        dialog = builder.create();
    }

    private void initializeDialog(MainActivity _activity) {
        activity = _activity;
        listOfPlaylists = activity.listOfPlaylists;
        currentPlaylist = activity.currentPlaylist;
        custom = activity.currentLopIndex == 0;

        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle(R.string.add_to_playlist);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
        recyclerView = new RecyclerView(activity);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setVerticalScrollBarEnabled(true);

        builder.setView(recyclerView);
    }

    private void copyRadioStation() {
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
            if (contains[i]) {
                playlist.addRadioStation(station);
                if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.playingRadioStationIndex++;
            }
        }
    }

    private void copyRadioStations() {
        for (int i = 0; i < length; i++) {
            if (contains[i]) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
                for (int j = forRadioStations.size() - 1; j >= 0; j--) {
                    RadioStation radioStation = activity.currentPlaylist.getRadioStationAt(forRadioStations.get(j));
                    if (!playlist.contains(radioStation)) playlist.addRadioStation(radioStation);
                }
                if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.playingRadioStationIndex += forRadioStations.size();
            }
        }
    }

    public void show() {
        if (items.isEmpty()) activity.showMessage(R.string.no_playlist_found);
        else dialog.show();
    }

    private class ManagePlaylistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Integer[] icons = {
                R.drawable.baseline_featured_play_list_24,
                R.drawable.baseline_favorite_24,
                R.drawable.baseline_library_music_24,
                R.drawable.baseline_newspaper_24,
                R.drawable.baseline_theater_comedy_24};
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(activity.getLayoutInflater().inflate(R.layout.playlist_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getAdapterPosition();
            LinearLayout layout = view.findViewById(R.id.layout);
            CheckBox checkBox = view.findViewById(R.id.checkBox);
            ImageView icon = view.findViewById(R.id.playlistIcon);
            TextView title = view.findViewById(R.id.playlistTitle);
            TextView size = view.findViewById(R.id.playlistSize);

            icon.setBackgroundResource(contains[pos] ? R.drawable.playlist_icon_selected
                            : R.drawable.playlist_icon);

            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(pos));
            icon.setImageResource(icons[playlist.icon]);
            title.setText(playlist.title);
            size.setText(String.format(activity.getString(R.string.n_stations), playlist.getLength()));
            checkBox.setChecked(contains[pos]);
            setOnClickListener(layout, pos);
        }

        @Override
        public int getItemCount() {
            return length;
        }

        private void setOnClickListener(View view, int position) {
            view.setOnClickListener(v -> {
                contains[position] = !contains[position];
                notifyItemChanged(position);
            });
        }
    }
}