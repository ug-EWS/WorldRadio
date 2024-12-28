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
    boolean[] contains;
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

        builder.setPositiveButton(R.string.dialog_button_add, (dialog1, which) -> {
            for (int i = 0; i < length2; i++) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
                if (contains[i]) {
                    playlist.addRadioStation(station);
                    if (i == activity.currentPlaylistIndex) activity.playlistAdapter.insertItem(0);
                }
            }
            activity.showMessage("Eklendi");
        });
        dialog = builder.create();
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forRadioStations) {
        initializeDialog(_activity);

        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(i);
            items.add(playlist.title);
        }

        itemsArr = items.toArray(new CharSequence[0]);
        contains = new boolean[length];

        builder.setMultiChoiceItems(itemsArr, null, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(R.string.dialog_button_add, (dialog1, which) -> {
            for (int i = 0; i < length; i++) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(i);
                if (contains[i]) {
                    for (int j = _forRadioStations.size() - 1; j >= 0; j--) {
                        playlist.addRadioStation(activity.currentPlaylist.getRadioStationAt(j));
                    }
                }
            }
            activity.showMessage("Eklendi");
            activity.setSelectionMode(false);
        });
        dialog = builder.create();
    }

    private void initializeDialog(MainActivity _activity) {
        activity = _activity;
        listOfPlaylists = activity.listOfPlaylists;
        currentPlaylist = activity.currentPlaylist;

        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle("Oynatma listesine ekle");
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
    }

    public void show() {
        dialog.show();
    }
}
