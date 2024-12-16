package com.example.worldradio;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

class ListOfPlaylistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
    MainActivity activity;

    ListOfPlaylistsAdapter(MainActivity _activity) {
        super();
        activity = _activity;
    }

    Integer[] icons = {
            R.drawable.baseline_featured_play_list_24,
            R.drawable.baseline_favorite_24,
            R.drawable.baseline_library_music_24,
            R.drawable.baseline_newspaper_24,
            R.drawable.baseline_theater_comedy_24};

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = activity.getLayoutInflater();
        View itemView = inf.inflate(R.layout.playlist_item, parent, false);
        return new RecyclerView.ViewHolder(itemView) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();

        ImageView icon = itemView.findViewById(R.id.playlistIcon);
        TextView title = itemView.findViewById(R.id.playlistTitle);
        TextView size = itemView.findViewById(R.id.playlistSize);
        ImageView options = itemView.findViewById(R.id.playlistOptions);

        setItemOnClickListener(itemView, pos);

        itemView.setBackgroundResource(activity.playingPlaylistIndex == pos ? R.drawable.list_item_playing : R.drawable.list_item);

        int iconIndex = activity.listOfPlaylists.getPlaylistAt(pos).icon;
        if (iconIndex > 4 || iconIndex < 0) iconIndex = 0;
        icon.setImageResource(icons[iconIndex]);
        title.setText(activity.listOfPlaylists.getPlaylistAt(pos).title);
        size.setText(String.valueOf(activity.listOfPlaylists.getPlaylistAt(pos).getLength()).concat(" kanal"));
        title.setTextColor(activity.playingPlaylistIndex == pos ? Color.GREEN : Color.WHITE);

        PopupMenu popupMenu = activity.getPlaylistPopupMenu(options, pos);

        options.setOnClickListener(view -> popupMenu.show());
    }

    @Override
    public int getItemCount() {
        return activity.listOfPlaylists.getLength();
    }

    public void insertItem(int index) {
        if (activity.playingPlaylist != null && index <= activity.playingPlaylistIndex) {
            activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);
        }
        this.notifyItemInserted(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength() - index);
    }

    public void removeItem(int index) {
        if (activity.playingPlaylist != null) {
            if (index < activity.playingPlaylistIndex) {
                activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);
            }
            if (index == activity.playingPlaylistIndex) {
                activity.closePlayer();
                activity.playingPlaylist = null;
            }
        }
        this.notifyItemRemoved(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }

    private void setItemOnClickListener(View v, int position) {
        v.setOnClickListener(view -> activity.openPlaylist(position));
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        activity.listOfPlaylists.movePlaylist(fromPosition, toPosition);
        int positionMin = Math.min(fromPosition, toPosition);
        int positionMax = Math.max(fromPosition, toPosition);

        if (activity.playingPlaylist != null)
            activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);

        notifyItemMoved(fromPosition, toPosition);
        notifyItemRangeChanged(positionMin, positionMax - positionMin + 1);
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
        activity.vibrator.vibrate(50);
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
    }
}