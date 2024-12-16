package com.example.worldradio;

import androidx.appcompat.app.AlertDialog;

class ManagePlaylistsDialog {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    RadioStation station;

    ManagePlaylistsDialog(MainActivity _activity, RadioStation _station) {
        activity = _activity;

        station = _station;

        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle(station.title);

        int length = activity.listOfPlaylists.getLength();
        CharSequence[] items = new CharSequence[length];
        boolean[] contains = new boolean[length];
        for (int i = 0; i < length; i++) {
            Playlist playlist = activity.listOfPlaylists.getPlaylistAt(i);
            items[i] = playlist.title;
            contains[i] = playlist.contains(station);
        }
        builder.setMultiChoiceItems(items, contains, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(R.string.dialog_button_apply, (dialog1, which) -> {
            for (int i = 0; i < length; i++) {
                Playlist playlist = activity.listOfPlaylists.getPlaylistAt(i);
                if (contains[i] && !playlist.contains(station)) {
                    playlist.addRadioStation(station);
                    if (i == activity.currentPlaylistIndex) activity.playlistAdapter.insertItem(0);
                }
                else if (!contains[i] && playlist.contains(station)) {
                    int index = playlist.getIndexOf(station);
                    playlist.removeRadioStation(index);
                    if (i == activity.currentPlaylistIndex) activity.playlistAdapter.removeItem(index);
                }
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.dialog_button_cancel, (dialog1, which) -> dialog.dismiss());
        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }
}
