package com.example.worldradio;

import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.RadioBrowser;

class PlaylistDialog {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    View dialogView;
    EditText editText;
    LinearLayout iconSelector;
    Playlist toEdit;
    int whereToAdd;
    int selectedIcon;

    PlaylistDialog(MainActivity _activity, int _forPlaylist) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        dialogView = activity.getLayoutInflater().inflate(R.layout.add_playlist, null);
        editText = dialogView.findViewById(R.id.editText);
        iconSelector = dialogView.findViewById(R.id.iconSelector);
        View.OnClickListener onClickListener = view -> selectIcon(iconSelector.indexOfChild(view));
        for (int i = 0; i < 5; i++) {
            iconSelector.getChildAt(i).setOnClickListener(onClickListener);
        }
        builder.setNegativeButton(activity.getString(R.string.dialog_button_cancel), null);
        builder.setOnDismissListener(dialog1 -> OnlinePlaylistsUtils.hideKeyboard(activity, editText));
        builder.setView(dialogView);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        boolean _newPlaylist = _forPlaylist == -1;
        builder.setTitle(activity.getString(_newPlaylist ? R.string.add_playlist : R.string.edit_playlist));
        if (_newPlaylist) {
            resetView();
            builder.setPositiveButton(activity.getString(R.string.dialog_button_add), (dialog, which) -> {
                String text = editText.getText().toString();
                if (text.isEmpty()) text = editText.getHint().toString();
                activity.listOfPlaylists.addPlaylistTo(new Playlist(text, selectedIcon), whereToAdd);
                activity.listOfPlaylistsAdapter.insertItem(whereToAdd);
                activity.updateNoItemsView();
                activity.listOfPlaylistsRecycler.scrollToPosition(whereToAdd);
                resetView();
            });
        } else {
            toEdit = activity.listOfPlaylists.getPlaylistAt(_forPlaylist);
            editText.setText(toEdit.title);
            selectIcon(toEdit.icon);
            builder.setPositiveButton(activity.getString(R.string.dialog_button_apply), (dialog, which) -> {
                String text = editText.getText().toString();
                toEdit.title = text;
                toEdit.icon = selectedIcon;
                activity.listOfPlaylistsAdapter.notifyItemChanged(_forPlaylist);
                if (activity.playlistOpen) activity.titleText.setText(text);
            });
        }

        dialog = builder.create();
    }

    private void resetView() {
        selectIcon(0);
        editText.setText("");
    }

    private void selectIcon(int index) {
        if (index > 4 || index < 0) index = 0;
        selectedIcon = index;
        for (int i = 0; i < 5; i++) {
            ImageView icon = (ImageView) iconSelector.getChildAt(i);
            icon.setBackgroundResource(i == selectedIcon ? R.drawable.playlist_icon : 0);
            icon.setColorFilter(activity.getColor(i == selectedIcon ? R.color.teal_700 : R.color.grey6));
        }
    }

    public void show() {
        show(0);
    }

    public void show(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        dialog.show();
        OnlinePlaylistsUtils.showKeyboard(activity, editText);
    }
}