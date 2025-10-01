package com.example.worldradio;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

class ManagePlaylistsDialog extends BottomSheetDialog {
    MainActivity activity;

    RecyclerView recyclerView;
    LinearLayout newPlaylistButton;
    ImageView cancelButton;
    TextView moveButton;
    TextView copyButton;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    RadioStation station;

    ArrayList<Integer> originalIndexes;
    ArrayList<Integer> forRadioStations;
    ArrayList<Integer> selectedItems;
    boolean custom;
    int length;

    ManagePlaylistsDialog(MainActivity _activity, int _forRadioStation) {
        super(_activity, R.style.BottomSheetDialogTheme);
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
        selectedItems = new ArrayList<>();
        if (length == 1) selectedItems.add(0);
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

        copyButton.setOnClickListener(v -> {
            dismiss();
            activity.setSelectionMode(false);
            if (copyRadioStation()) activity.showMessage(custom ? R.string.copied : R.string.added);
            else activity.showMessage(R.string.playlist_not_selected);
        });

        if (custom) {
            moveButton.setOnClickListener(v -> {
                dismiss();
                activity.setSelectionMode(false);
                if (copyRadioStation()) {
                    currentPlaylist.removeRadioStation(_forRadioStation);
                    activity.playlistAdapter.removeItem(_forRadioStation);
                    activity.showMessage(R.string.moved);
                } else activity.showMessage(R.string.playlist_not_selected);
            });
        }
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forRadioStations) {
        super(_activity, R.style.BottomSheetDialogTheme);
        initializeDialog(_activity);

        originalIndexes = new ArrayList<>();
        forRadioStations = _forRadioStations;

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            if (activity.currentLopIndex != 0 || activity.currentPlaylistIndex != i)
                originalIndexes.add(i);
        }
        length = originalIndexes.size();

        selectedItems = new ArrayList<>();
        if (length == 1) selectedItems.add(0);
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

        copyButton.setOnClickListener(v -> {
            dismiss();
            activity.setSelectionMode(false);
            if (copyRadioStations()) activity.showMessage(custom ? R.string.copied : R.string.added);
            else activity.showMessage(R.string.playlist_not_selected);
        });

        if (custom) {
            moveButton.setOnClickListener(v -> {
                dismiss();
                activity.setSelectionMode(false);
                if (copyRadioStations()) {
                    currentPlaylist.removeRadioStations(forRadioStations);
                    activity.showMessage(R.string.moved);
                } else activity.showMessage(R.string.playlist_not_selected);
            });
        }
    }

    private void initializeDialog(MainActivity _activity) {
        activity = _activity;
        listOfPlaylists = activity.listOfPlaylists;
        currentPlaylist = activity.currentPlaylist;
        custom = activity.currentLopIndex == 0;

        View dialogView = getLayoutInflater().inflate(R.layout.manage_playlists, null);
        recyclerView = dialogView.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        newPlaylistButton = dialogView.findViewById(R.id.newPlaylistButton);
        newPlaylistButton.setOnClickListener(v ->
                new PlaylistDialog(activity, this).show());

        cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> cancel());

        moveButton = dialogView.findViewById(R.id.moveButton);
        copyButton = dialogView.findViewById(R.id.copyButton);

        setContentView(dialogView);
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void show() {
        refreshButtons();
        super.show();
    }

    private boolean copyRadioStation() {
        boolean copied = false;
        for (Integer i : selectedItems) {
            listOfPlaylists.getPlaylistAt(originalIndexes.get(i)).addRadioStation(station);
            if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.playingRadioStationIndex++;
            copied = true;
        }
        return copied;
    }

    private boolean copyRadioStations() {
        boolean copied = false;
        for (Integer i : selectedItems) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
            for (int j = forRadioStations.size() - 1; j >= 0; j--) {
                RadioStation radioStation = activity.currentPlaylist.getRadioStationAt(forRadioStations.get(j));
                if (!playlist.contains(radioStation)) playlist.addRadioStation(radioStation);
            }
            if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.playingRadioStationIndex += forRadioStations.size();
            copied = true;
        }
        return copied;
    }

    public void refresh() {
        selectedItems.replaceAll(integer -> integer + 1);
        selectedItems.add(0, 0);
        originalIndexes.replaceAll(integer -> integer + 1);
        originalIndexes.add(0, 0);
        length++;
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
            recyclerView.getAdapter().notifyItemRangeChanged(1, length - 1);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        moveButton.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE);
        copyButton.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE);
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
            int pos = holder.getBindingAdapterPosition();
            LinearLayout layout = view.findViewById(R.id.layout);
            ImageView icon = view.findViewById(R.id.playlistIcon);
            TextView title = view.findViewById(R.id.playlistTitle);
            TextView size = view.findViewById(R.id.playlistSize);

            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(pos));
            icon.setImageResource(selectedItems.contains(pos) ? R.drawable.baseline_done_24 : icons[playlist.icon]);
            icon.setBackgroundResource(selectedItems.contains(pos) ? R.drawable.playlist_icon_selected : R.drawable.playlist_icon);
            title.setText(playlist.title);
            size.setText(String.format(activity.getString(R.string.n_stations), playlist.getLength()));
            setOnClickListener(layout, pos);
        }

        @Override
        public int getItemCount() {
            return length;
        }

        private void setOnClickListener(View view, int position) {
            view.setOnClickListener(v -> {
                if (selectedItems.contains(position)) selectedItems.remove((Integer) position);
                else selectedItems.add(position);
                notifyItemChanged(position);
                refreshButtons();
            });
        }
    }
}