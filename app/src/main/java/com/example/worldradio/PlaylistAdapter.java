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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getBindingAdapterPosition();
        boolean playing = activity.currentLopIndex == activity.playingLopIndex && activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStationIndex == pos;
        boolean selected = activity.selectionMode && activity.selectedItems.contains(pos);
        boolean notSelected = activity.selectionMode && !activity.selectedItems.contains(pos);

        LinearLayout layout = itemView.findViewById(R.id.layout);
        ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
        TextView title = itemView.findViewById(R.id.videoTitle);
        ImageView options = itemView.findViewById(R.id.radioAddTo);
        ImageView trendBadge = itemView.findViewById(R.id.trendBadge);
        TextView codecText = itemView.findViewById(R.id.codecText);
        TextView bitrateText = itemView.findViewById(R.id.bitrateText);

        setItemOnClickListener(layout, pos);
        setItemOnLongClickListener(layout, pos);

        layout.setAlpha(notSelected ? 0.5F : 1.0F);

        thumbnail.setBackgroundResource(
                selected ? R.drawable.playlist_icon_selected
                        : playing ? R.drawable.playlist_icon_playing
                        : R.drawable.playlist_icon);
        title.setTextColor(activity.getColor(playing ? R.color.green2 : R.color.grey1));

        RadioStation thisStation = activity.currentPlaylist.getRadioStationAt(pos);

        codecText.setVisibility(activity.showCodec ? View.VISIBLE
                : (activity.currentPlaylist.sortBy == Playlist.CODEC ? View.VISIBLE : View.GONE));
        codecText.setText(thisStation.codec);

        bitrateText.setVisibility(
                (activity.showBitrate || activity.currentPlaylist.sortBy == Playlist.BITRATE) && thisStation.bitrate != 0 ? View.VISIBLE : View.GONE);
        bitrateText.setText(String.format(Locale.ENGLISH, "%d kB/s", thisStation.bitrate));

        int type = activity.currentPlaylist.type;
        if (activity.searchMode && activity.foundItemIndex == pos) {
            SpannableString spannableString = new SpannableString(thisStation.title);
            spannableString.setSpan(new ForegroundColorSpan(activity.getColor(R.color.yellow)),
                    activity.foundAtStart,
                    activity.foundAtEnd,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            title.setText(spannableString, TextView.BufferType.SPANNABLE);
        } else title.setText(thisStation.title);

        if (selected) thumbnail.setImageResource(R.drawable.baseline_done_24);
        else if (thisStation.faviconUrl.isEmpty()) thumbnail.setImageResource(R.drawable.baseline_radio_24);
        else Glide.with(activity).load(thisStation.faviconUrl).placeholder(R.drawable.baseline_radio_24).into(thumbnail);

        trendBadge.setVisibility(View.GONE);

        if ((type == 1 || type == 6) && position < 3) {
            trendBadge.setImageResource(List.of(R.drawable.baseline_1k_24, R.drawable.baseline_2k_24, R.drawable.baseline_3k_24).get(position));
            trendBadge.setVisibility(View.VISIBLE);
        }

        options.setVisibility(activity.selectionMode || activity.listSortMode || activity.searchMode ? View.INVISIBLE : View.VISIBLE);

        BottomSheetMenu bottomSheetMenu = activity.getRadioStationPopupMenu(pos);
        options.setOnClickListener(view -> bottomSheetMenu.showMenu());

        options.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) options.setBackgroundResource(R.drawable.ripple_on_dark_grey);
            return false;
        });

        layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) options.setBackgroundResource(0);
            return false;
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

        if (activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStation != null)
            activity.playingRadioStationIndex = activity.currentPlaylist.getIndexOf(activity.playingRadioStation);

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
        int position = viewHolder.getBindingAdapterPosition();
        if (i == ItemTouchHelper.START) {
            new ManagePlaylistsDialog(activity, position).show();
            notifyItemChanged(position);
        }
        else activity.removeRadioStation(position);
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