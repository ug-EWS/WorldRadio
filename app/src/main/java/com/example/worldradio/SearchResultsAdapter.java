package com.example.worldradio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    RadioStationDialog dialog;
    MainActivity activity;
    Playlist playlist;

    SearchResultsAdapter(RadioStationDialog _dialog) {
        super();
        dialog = _dialog;
        activity = dialog.activity;
        playlist = dialog.resultPlaylist;
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
        RadioStation station = dialog.resultPlaylist.getRadioStationAt(pos);
        title.setText(station.title);
        Glide.with(activity).load(station.faviconUrl).into(thumbnail);
        options.setVisibility(View.GONE);
        options.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return dialog.resultPlaylist.getLength();
    }

    private void setItemOnClickListener(View v, int position) {
        v.setOnClickListener(view -> {
            RadioStation _station = dialog.resultPlaylist.getRadioStationAt(position);
            if (activity.currentPlaylist.contains(_station)) {
                activity.showMessage("Bu radyo kanalı zaten eklenmiş.");
            } else {
                activity.currentPlaylist.addRadioStationTo(_station, dialog.whereToAdd);
                activity.playlistAdapter.insertItem(dialog.whereToAdd);
                dialog.dialog.dismiss();
            }
        });
    }
}
