package com.example.worldradio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();
        Playlist playlist = activity.currentLop.getPlaylistAt(pos);
        boolean playing = activity.playingLopIndex == activity.currentLopIndex && activity.playingPlaylistIndex == pos;
        boolean selected = activity.selectionMode && activity.selectedItems.contains(pos);
        boolean notSelected = activity.selectionMode && !activity.selectedItems.contains(pos);

        LinearLayout layout = itemView.findViewById(R.id.layout);
        ImageView icon = itemView.findViewById(R.id.playlistIcon);
        TextView title = itemView.findViewById(R.id.playlistTitle);
        TextView size = itemView.findViewById(R.id.playlistSize);
        ImageView options = itemView.findViewById(R.id.playlistOptions);

        setItemOnClickListener(layout, pos);

        layout.setAlpha(notSelected ? 0.5F : 1.0F);

        icon.setBackgroundResource(
                selected ? R.drawable.playlist_icon_selected
                        : playing ? R.drawable.playlist_icon_playing
                        : R.drawable.playlist_icon);

        int type = playlist.type;
        if (type == 2) Glide.with(activity)
                .load(String.format("https://flagsapi.com/%s/shiny/64.png", playlist.countryCode))
                .placeholder(R.drawable.baseline_public_24).into(icon);
        else {
            int iconIndex = playlist.icon;
            if (iconIndex > 4 || iconIndex < 0) iconIndex = 0;
            icon.setImageResource(selected ? R.drawable.baseline_done_24
                    : type == 1 ? R.drawable.baseline_trending_up_24
                    : type == 6 ? R.drawable.baseline_star_24
                    : type == 3 ? R.drawable.baseline_language_24
                    : type == 4 ? R.drawable.baseline_tag_24
                    : type == 7 ? R.drawable.baseline_new_releases_24
                    : type == 8 ? R.drawable.baseline_headphones_24
                    : icons[iconIndex]);
        }

        String titleStr = playlist.title;
        if (activity.searchMode && activity.foundItemIndex == pos) {
            SpannableString spannableString = new SpannableString(titleStr);
            spannableString.setSpan(new ForegroundColorSpan(activity.getColor(R.color.yellow)),
                    activity.foundAtStart,
                    activity.foundAtEnd,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            title.setText(spannableString, TextView.BufferType.SPANNABLE);
        } else title.setText(titleStr);
        size.setText(String.format(activity.getString(R.string.n_stations), playlist.getLength()));
        title.setTextColor(activity.getColor(playing ? R.color.green2 : R.color.grey1));

        options.setVisibility(activity.selectionMode || activity.listSortMode || activity.searchMode || activity.currentLopIndex != 0 ? View.INVISIBLE : View.VISIBLE);

        if (activity.currentLopIndex == 0) {
            PopupMenu popupMenu = activity.getPlaylistPopupMenu(options, false, pos);
            options.setOnClickListener(view -> popupMenu.show());
            setItemOnLongClickListener(layout, pos);

            options.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) options.setBackgroundResource(R.drawable.ripple_on_dark_grey);
                return false;
            });

            layout.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) options.setBackgroundResource(0);
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return activity.currentLop.getLength();
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
        v.setOnClickListener(view -> {
                if (activity.selectionMode) {
                    if (activity.selectedItems.contains(position))
                        activity.selectedItems.remove((Integer) position);
                    else activity.selectedItems.add(position);
                    if (activity.selectedItems.isEmpty()) {
                        activity.setSelectionMode(false);
                    } else {
                        notifyItemChanged(position);
                        activity.updateToolbar();
                    }
                } else activity.openPlaylist(position);
                if (activity.searchMode) activity.setSearchMode(false);
        });
    }

    private void setItemOnLongClickListener(View _view, int position) {
        _view.setOnLongClickListener(view -> {
            if (!(activity.selectionMode || activity.listSortMode || activity.searchMode || activity.currentLopIndex != 0)) {
                activity.selectedItems = new ArrayList<>();
                activity.selectedItems.add(position);
                activity.setSelectionMode(true);
            }
            return true;
        });
    }

    @Override
    public boolean isSwipeEnabled() {
        return !activity.selectionMode && activity.currentLopIndex == 0;
    }

    @Override
    public boolean isDragEnabled() {
        return activity.listSortMode;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        activity.listOfPlaylists.movePlaylist(fromPosition, toPosition);
        if (activity.playingPlaylist != null)
            activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);

        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
        ((FrameLayout)viewHolder.itemView).getChildAt(0).setBackgroundResource(R.drawable.ripple_list_drag);
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
        ((FrameLayout)viewHolder.itemView).getChildAt(0).setBackgroundResource(R.drawable.ripple_list);
    }

    @Override
    public void onSwipe(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        int position = viewHolder.getAdapterPosition();
        if (i == ItemTouchHelper.START) {
            activity.sharePlaylist(activity.listOfPlaylists.getPlaylistAt(position));
            notifyItemChanged(position);
        }
        else activity.removePlaylist(position);
    }

    @Override
    public boolean isPlaylistOpen() {
        return activity.playlistOpen;
    }

    @Override
    public Context getContext() {
        return activity;
    }
}