package com.example.worldradio;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
    MainActivity activity;

    PlaylistAdapter(MainActivity _activity) {
        super();
        activity = _activity;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = activity.getLayoutInflater();
        View itemView = inf.inflate(R.layout.radio_item, parent, false);
        return new RecyclerView.ViewHolder(itemView) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();
        boolean playing = activity.currentLopIndex == activity.playingLopIndex && activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStationIndex == pos;

        LinearLayout layout = itemView.findViewById(R.id.layout);
        ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
        TextView title = itemView.findViewById(R.id.videoTitle);
        ImageView options = itemView.findViewById(R.id.radioAddTo);

        setItemOnClickListener(layout, pos);
        setItemOnLongClickListener(layout, pos);

        layout.setAlpha(activity.selectionMode && !activity.selectedItems.contains(pos) ? 0.5F : 1.0F);

        thumbnail.setBackgroundResource(
                activity.selectionMode && activity.selectedItems.contains(pos) ? R.drawable.playlist_icon_selected
                        : playing ? R.drawable.playlist_icon_playing
                        : R.drawable.playlist_icon);
        title.setTextColor(activity.getColor(playing ? R.color.green2 : R.color.grey1));

        RadioStation thisVideo = activity.currentPlaylist.getRadioStationAt(pos);
        if (activity.searchMode && activity.foundItemIndex == pos) {
            SpannableString spannableString = new SpannableString(thisVideo.title);
            spannableString.setSpan(new ForegroundColorSpan(activity.getColor(R.color.yellow)),
                    activity.foundAtStart,
                    activity.foundAtEnd,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            title.setText(spannableString, TextView.BufferType.SPANNABLE);
        } else title.setText(thisVideo.title);

        if (thisVideo.faviconUrl.isEmpty()) thumbnail.setImageResource(R.drawable.baseline_radio_24);
        else Glide.with(activity).load(thisVideo.faviconUrl).into(thumbnail);

        options.setVisibility(activity.selectionMode || activity.listSortMode || activity.searchMode ? View.INVISIBLE : View.VISIBLE);
        options.setImageResource(activity.currentLopIndex == 0 ? R.drawable.baseline_more_vert_24 : R.drawable.baseline_playlist_add_24);

        PopupMenu popupMenu = activity.getRadioStationPopupMenu(options, pos);
        options.setOnClickListener(view -> {
            if (activity.currentLopIndex == 0) popupMenu.show();
            else new ManagePlaylistsDialog(activity, pos).show();
        });
    }

    @Override
    public int getItemCount() {
        return activity.currentPlaylist.getLength();
    }

    public void insertItem(int index) {
        if (activity.playingRadioStation != null && index <= activity.playingRadioStationIndex && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            activity.playingRadioStationIndex++;
            activity.playingRadioStation = activity.currentPlaylist.getRadioStationAt(activity.playingRadioStationIndex);
        }
        this.notifyItemInserted(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength() - index);
    }

    public void removeItem(int index) {
        if (activity.playingRadioStation != null && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            if (index < activity.playingRadioStationIndex) {
                activity.playingRadioStationIndex = activity.currentPlaylist.getIndexOf(activity.playingRadioStation);
            }
            if (index == activity.playingRadioStationIndex) {
                activity.closePlayer();
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
            } else if (!activity.listSortMode) {
                if (activity.currentLopIndex == activity.playingLopIndex
                        && activity.currentPlaylistIndex == activity.playingPlaylistIndex
                        && position == activity.playingRadioStationIndex) {
                    if (activity.isPlaying) activity.exoPlayer.pause();
                    else activity.exoPlayer.play();
                } else activity.playRadioStation(position, true, true);
            }
            if (activity.searchMode) activity.setSearchMode(false);
        });
    }


    private void setItemOnLongClickListener(View _view, int position) {
        _view.setOnLongClickListener(view -> {
            if (!(activity.selectionMode || activity.listSortMode || activity.searchMode)) {
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
        activity.currentPlaylist.moveRadioStation(fromPosition, toPosition);

        //int positionMin = Math.min(fromPosition, toPosition);
        //int positionMax = Math.max(fromPosition, toPosition);

        if (activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStation != null)
            activity.playingRadioStationIndex = activity.currentPlaylist.getIndexOf(activity.playingRadioStation);

        notifyItemMoved(fromPosition, toPosition);
        //notifyItemRangeChanged(positionMin, positionMax - positionMin + 1);

    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void onSwipe(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        activity.removeRadioStation(viewHolder.getAdapterPosition());
    }

    @Override
    public Context getContext() {
        return activity;
    }
}