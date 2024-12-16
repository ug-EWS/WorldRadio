package com.example.worldradio;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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

        ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
        TextView title = itemView.findViewById(R.id.videoTitle);
        ImageView options = itemView.findViewById(R.id.radioAddTo);

        setItemOnClickListener(itemView, pos);

        itemView.setBackgroundResource(activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStationIndex == pos
                ? R.drawable.list_item_playing
                : R.drawable.list_item);

        RadioStation thisVideo = activity.currentPlaylist.getRadioStationAt(pos);
        title.setText(thisVideo.title);
        title.setTextColor(activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStationIndex == pos ? Color.GREEN : Color.WHITE);
        if (thisVideo.faviconUrl.isEmpty()) thumbnail.setImageResource(R.drawable.baseline_radio_24);
        else Glide.with(activity).load(thisVideo.faviconUrl).into(thumbnail);

        options.setOnClickListener(getButtonOnClickListener(position));
    }

    @Override
    public int getItemCount() {
        return activity.currentPlaylist.getLength();
    }

    public void insertItem(int index) {
        if(activity.playingRadioStation != null && index <= activity.playingRadioStationIndex && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            activity.playingRadioStationIndex++;
            activity.playingRadioStation = activity.currentPlaylist.getRadioStationAt(activity.playingRadioStationIndex);
        }
        this.notifyItemInserted(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength() - index);
    }

    public void removeItem(int index) {
        if(activity.playingRadioStation != null && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            if(index < activity.playingRadioStationIndex) {
                activity.playingRadioStationIndex = activity.currentPlaylist.getIndexOf(activity.playingRadioStation);
            }
            if(index == activity.playingRadioStationIndex) {
                activity.closePlayer();
            }
        }
        this.notifyItemRemoved(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }

    private void setItemOnClickListener(View v, int position) {
        v.setOnClickListener(view -> {
            if (!(activity.currentPlaylistIndex == activity.playingPlaylistIndex && position == activity.playingRadioStationIndex))
                activity.playRadioStation(position);});
    }

    private View.OnClickListener getButtonOnClickListener(int position) {
        return view -> new ManagePlaylistsDialog(activity, activity.currentPlaylist.getRadioStationAt(position)).show();
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        activity.currentPlaylist.moveRadioStation(fromPosition, toPosition);

        int positionMin = Math.min(fromPosition, toPosition);
        int positionMax = Math.max(fromPosition, toPosition);

        if (activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingRadioStation != null)
            activity.playingRadioStationIndex = activity.currentPlaylist.getIndexOf(activity.playingRadioStation);

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
