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
    LinearLayout newPlaylistButton;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    RadioStation station;


    ArrayList<Integer> originalIndexes;
    ArrayList<Integer> forRadioStations;
    ArrayList<Boolean> contains;
    boolean custom;
    int length;

    ManagePlaylistsDialog(MainActivity _activity, int _forRadioStation) {
        initializeDialog(_activity);
        station = currentPlaylist.getRadioStationAt(_forRadioStation);

        originalIndexes = new ArrayList<>();

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(i);
            if (!playlist.contains(station))
                originalIndexes.add(i);
        }

        length = originalIndexes.size();
        contains = new ArrayList<>();
        if (length == 1) contains.add(true);
        else for (int i = 0; i < length; i++) contains.add(false);

        recyclerView.setAdapter(new ManagePlaylistsAdapter());

        builder.setPositiveButton(custom ? R.string.copy : R.string.dialog_button_add, (dialog1, which) -> {
            if (copyRadioStation()) activity.showMessage(custom ? activity.getString(R.string.copied) : "Eklendi.");
            else activity.showMessage("Oynatma listesi seçilmedi.");
        });

        if (custom) {
            builder.setNeutralButton(R.string.move, (dialog1, which) -> {
                if (copyRadioStation()) {
                    currentPlaylist.removeRadioStation(_forRadioStation);
                    activity.playlistAdapter.removeItem(_forRadioStation);
                    activity.showMessage(R.string.moved);
                } else activity.showMessage("Oynatma listesi seçilmedi.");
            });
        }
        dialog = builder.create();
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forRadioStations) {
        initializeDialog(_activity);

        originalIndexes = new ArrayList<>();
        forRadioStations = _forRadioStations;

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            if (activity.currentLopIndex != 0 || activity.currentPlaylistIndex != i)
                originalIndexes.add(i);
        }
        length = originalIndexes.size();

        contains = new ArrayList<>();
        if (length == 1) contains.add(true);
        else for (int i = 0; i < length; i++) contains.add(false);
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

        builder.setPositiveButton(custom ? R.string.copy : R.string.dialog_button_add, (dialog1, which) -> {
            activity.setSelectionMode(false);
            if (copyRadioStations()) activity.showMessage(custom ? activity.getString(R.string.copied) : "Eklendi.");
            else activity.showMessage("Oynatma listesi seçilmedi.");
        });

        if (custom) {
            builder.setNeutralButton(R.string.move, (dialog1, which) -> {
                activity.setSelectionMode(false);
                if (copyRadioStations()) {
                    currentPlaylist.removeRadioStations(forRadioStations);
                    activity.showMessage(R.string.moved);
                } else activity.showMessage("Oynatma listesi seçilmedi.");
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
        View dialogView = activity.getLayoutInflater().inflate(R.layout.manage_playlists, null);
        recyclerView = dialogView.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        newPlaylistButton = dialogView.findViewById(R.id.newPlaylistButton);
        newPlaylistButton.setOnClickListener(v ->
                new PlaylistDialog(activity, this).show());

        builder.setView(dialogView);
    }

    private boolean copyRadioStation() {
        boolean copied = false;
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
            if (contains.get(i)) {
                playlist.addRadioStation(station);
                if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.playingRadioStationIndex++;
                copied = true;
            }
        }
        return copied;
    }

    private boolean copyRadioStations() {
        boolean copied = false;
        for (int i = 0; i < length; i++) {
            if (contains.get(i)) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
                for (int j = forRadioStations.size() - 1; j >= 0; j--) {
                    RadioStation radioStation = activity.currentPlaylist.getRadioStationAt(forRadioStations.get(j));
                    if (!playlist.contains(radioStation)) playlist.addRadioStation(radioStation);
                }
                if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.playingRadioStationIndex += forRadioStations.size();
                copied = true;
            }
        }
        return copied;
    }

    public void show() {
        dialog.show();
    }

    public void refresh() {
        contains.add(0, true);
        originalIndexes.replaceAll(integer -> integer + 1);
        originalIndexes.add(0, 0);
        length++;
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
            recyclerView.getAdapter().notifyItemRangeChanged(1, length - 1);
        }
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

            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(pos));
            icon.setImageResource(contains.get(pos) ? R.drawable.baseline_done_24 : icons[playlist.icon]);
            icon.setBackgroundResource(contains.get(pos) ? R.drawable.playlist_icon_selected : R.drawable.playlist_icon);
            title.setText(playlist.title);
            size.setText(String.format(activity.getString(R.string.n_stations), playlist.getLength()));
            checkBox.setChecked(contains.get(pos));
            setOnClickListener(layout, pos);
        }

        @Override
        public int getItemCount() {
            return length;
        }

        private void setOnClickListener(View view, int position) {
            view.setOnClickListener(v -> {
                contains.set(position, !contains.get(position));
                notifyItemChanged(position);
            });
        }
    }
}