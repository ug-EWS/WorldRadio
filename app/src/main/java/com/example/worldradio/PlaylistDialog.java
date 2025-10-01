package com.example.worldradio;

import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.RadioBrowser;

class PlaylistDialog extends BottomSheetDialog {
    MainActivity activity;
    ManagePlaylistsDialog managePlaylistsDialog;
    View dialogView;
    ImageView cancelButton;
    TextView addButton;
    TextView title;
    EditText editText;
    LinearLayout iconSelector;
    Playlist toEdit;
    int whereToAdd;
    int selectedIcon;

    PlaylistDialog(MainActivity _activity, int _forPlaylist) {
        super(_activity, R.style.BottomSheetDialogTheme);
        activity = _activity;
        dialogView = getLayoutInflater().inflate(R.layout.add_playlist, null);
        editText = dialogView.findViewById(R.id.editText);
        iconSelector = dialogView.findViewById(R.id.iconSelector);
        View.OnClickListener onClickListener = view -> selectIcon(iconSelector.indexOfChild(view));
        for (int i = 0; i < 5; i++) {
            iconSelector.getChildAt(i).setOnClickListener(onClickListener);
        }
        cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> cancel());
        title = dialogView.findViewById(R.id.title);
        addButton = dialogView.findViewById(R.id.addButton);
        setOnDismissListener(dialog1 -> OnlinePlaylistsUtils.hideKeyboard(activity, editText));
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                addButton.performClick();
                return true;
            }
            return false;
        });
        boolean _newPlaylist = _forPlaylist == -1;
        title.setText(activity.getString(_newPlaylist ? R.string.add_playlist : R.string.edit_playlist));
        if (_newPlaylist) resetView();
        else {
            toEdit = activity.listOfPlaylists.getPlaylistAt(_forPlaylist);
            editText.setText(toEdit.title);
            selectIcon(toEdit.icon);
        }
        addButton.setText(_newPlaylist ? R.string.dialog_button_add : R.string.dialog_button_apply);
        addButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            if (_newPlaylist) {
                if (text.isEmpty()) text = editText.getHint().toString();
                activity.listOfPlaylists.addPlaylistTo(new Playlist(text, selectedIcon), whereToAdd);
                activity.listOfPlaylistsAdapter.insertItem(whereToAdd);
                activity.updateNoItemsView();
                activity.listOfPlaylistsRecycler.scrollToPosition(whereToAdd);
                resetView();
                if (managePlaylistsDialog != null) managePlaylistsDialog.refresh();
            } else {
                toEdit.title = text;
                toEdit.icon = selectedIcon;
                activity.updateShortcutOfPlaylist(toEdit);
                activity.listOfPlaylistsAdapter.notifyItemChanged(_forPlaylist);
                if (activity.playlistOpen) activity.titleText.setText(text);
            }
            dismiss();
        });
        setContentView(dialogView);
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    PlaylistDialog(MainActivity _activity, ManagePlaylistsDialog _managePlaylistsDialog) {
        this(_activity, -1);
        managePlaylistsDialog = _managePlaylistsDialog;
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

    public void showDialog() {
        showDialog(0);
    }

    public void showDialog(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        show();
        OnlinePlaylistsUtils.showKeyboard(activity, editText);
    }
}