package com.example.worldradio;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Objects;

class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    RadioStationDialog dialog;
    MainActivity activity;
    Playlist resultPlaylist;

    SearchResultsAdapter(RadioStationDialog _dialog) {
        super();
        dialog = _dialog;
        activity = dialog.activity;
        resultPlaylist = dialog.resultPlaylist;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = activity.getLayoutInflater();
        View itemView = inf.inflate(R.layout.result_item, parent, false);
        return new RecyclerView.ViewHolder(itemView) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();

        ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
        TextView title = itemView.findViewById(R.id.videoTitle);

        setItemOnClickListener(itemView, pos);
        RadioStation station = resultPlaylist.getRadioStationAt(pos);
        title.setText(station.title);
        if (station.faviconUrl.isEmpty()) thumbnail.setImageResource(R.drawable.baseline_radio_24);
        else Glide.with(activity).load(station.faviconUrl).into(thumbnail);
    }

    @Override
    public int getItemCount() {
        return resultPlaylist.getLength();
    }

    private void setItemOnClickListener(View v, int position) {
        v.setOnClickListener(view -> {
            RadioStation _station = resultPlaylist.getRadioStationAt(position);
            if (activity.currentPlaylist.contains(_station)) {
                activity.showMessage(R.string.already_added);
            } else {
                activity.currentPlaylist.addRadioStationTo(_station, dialog.whereToAdd);
                activity.playlistAdapter.insertItem(dialog.whereToAdd);
                activity.updateNoItemsView();
                dialog.dialog.dismiss();
                activity.playlistRecycler.scrollToPosition(dialog.whereToAdd);
            }
        });
    }
}