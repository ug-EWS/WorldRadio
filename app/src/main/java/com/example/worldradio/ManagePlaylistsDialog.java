package com.example.worldradio;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

class ManagePlaylistsDialog {
    MainActivity activity;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    RadioStation station;

    ArrayList<CharSequence> items;
    ArrayList<Integer> originalIndexes;
    ArrayList<Integer> forRadioStations;
    boolean[] contains;
    boolean custom;
    CharSequence[] itemsArr;
    int length;
    int length2;

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

        itemsArr = items.toArray(new CharSequence[0]);
        length2 = items.size();
        contains = new boolean[length2];

        builder.setMultiChoiceItems(itemsArr, null, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(custom ? R.string.copy : R.string.dialog_button_add, (dialog1, which) -> {
            copyRadioStation();
            activity.showMessage(R.string.copied);
        });

        if (custom)
            builder.setNeutralButton(R.string.move, (dialog1, which) -> {
                copyRadioStation();
                currentPlaylist.removeRadioStation(_forRadioStation);
                activity.playlistAdapter.removeItem(_forRadioStation);
                activity.showMessage(R.string.moved);
            });
        dialog = builder.create();
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forRadioStations) {
        initializeDialog(_activity);

        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();
        forRadioStations = _forRadioStations;

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            if (activity.currentPlaylistIndex != i) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(i);
                items.add(playlist.title);
                originalIndexes.add(i);
            }
        }

        itemsArr = items.toArray(new CharSequence[0]);
        contains = new boolean[length];

        builder.setMultiChoiceItems(itemsArr, null, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(custom ? R.string.copy : R.string.dialog_button_add, (dialog1, which) -> {
            copyRadioStations();
            activity.showMessage(R.string.copied);
            activity.setSelectionMode(false);
        });

        if (custom)
            builder.setNeutralButton(R.string.move, (dialog1, which) -> {
                copyRadioStations();
                currentPlaylist.removeRadioStations(forRadioStations);
                activity.showMessage(R.string.moved);
                activity.setSelectionMode(false);
            });
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
    }

    private void copyRadioStation() {
        for (int i = 0; i < length2; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
            if (contains[i]) {
                playlist.addRadioStation(station);
                if (i == activity.currentPlaylistIndex) activity.playlistAdapter.insertItem(0);
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
            }
        }
    }

    public void show() {
        if (items.isEmpty()) activity.showMessage(R.string.no_playlist_found);
        else dialog.show();
    }
}