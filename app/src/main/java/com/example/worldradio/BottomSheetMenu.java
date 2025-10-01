package com.example.worldradio;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

public class BottomSheetMenu extends BottomSheetDialog {
    private ArrayList<Integer> menuIcons;
    private ArrayList<Integer> menuTitles;
    private ArrayList<Runnable> menuListeners;
    private ArrayList<Boolean> itemsEnabled;
    private ArrayList<String> menuDescriptions;
    private View dialogView;
    private LinearLayout separator;
    private RecyclerView recyclerView;
    private LinearLayout playlistLayout;
    private LinearLayout stationLayout;
    private LinearLayout sortLayout;
    private int selection;
    Integer[] icons = {
            R.drawable.baseline_featured_play_list_24,
            R.drawable.baseline_favorite_24,
            R.drawable.baseline_library_music_24,
            R.drawable.baseline_newspaper_24,
            R.drawable.baseline_theater_comedy_24};

    BottomSheetMenu(Context _context) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        playlistLayout.setVisibility(View.GONE);
        stationLayout.setVisibility(View.GONE);
        sortLayout.setVisibility(View.GONE);
        separator.setVisibility(View.GONE);
        selection = -1;
    }

    BottomSheetMenu(Context _context, Playlist _forPlaylist) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        playlistLayout.setVisibility(View.VISIBLE);
        stationLayout.setVisibility(View.GONE);
        sortLayout.setVisibility(View.GONE);
        separator.setVisibility(View.VISIBLE);
        selection = -1;
        ImageView playlistIcon = dialogView.findViewById(R.id.playlistIcon);
        TextView playlistTitle = dialogView.findViewById(R.id.playlistTitle);
        TextView playlistSize = dialogView.findViewById(R.id.playlistSize);
        playlistIcon.setImageResource(icons[_forPlaylist.icon]);
        playlistTitle.setText(_forPlaylist.title);
        playlistSize.setText(String.format(_context.getString(R.string.n_stations), _forPlaylist.getLength()));
    }

    BottomSheetMenu(Context _context, RadioStation _forRadioStation) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        playlistLayout.setVisibility(View.GONE);
        stationLayout.setVisibility(View.VISIBLE);
        sortLayout.setVisibility(View.GONE);
        separator.setVisibility(View.VISIBLE);
        selection = -1;
        ImageView stationIcon = dialogView.findViewById(R.id.stationIcon);
        TextView stationTitle = dialogView.findViewById(R.id.stationTitle);
        Glide.with(_context).load(_forRadioStation.faviconUrl).into(stationIcon);
        stationTitle.setText(_forRadioStation.title);
    }

    BottomSheetMenu(Context _context, int _selection) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        playlistLayout.setVisibility(View.GONE);
        stationLayout.setVisibility(View.GONE);
        sortLayout.setVisibility(View.VISIBLE);
        separator.setVisibility(View.VISIBLE);
        selection = _selection;
        ImageView cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> cancel());
    }

    private void prepare() {
        dialogView = getLayoutInflater().inflate(R.layout.menu_dialog, null);
        separator = dialogView.findViewById(R.id.separator);
        recyclerView = dialogView.findViewById(R.id.recycler);
        playlistLayout = dialogView.findViewById(R.id.playlistLayout);
        stationLayout = dialogView.findViewById(R.id.stationLayout);
        sortLayout = dialogView.findViewById(R.id.sortLayout);

        menuIcons = new ArrayList<>();
        menuTitles = new ArrayList<>();
        menuListeners = new ArrayList<>();
        itemsEnabled = new ArrayList<>();
        menuDescriptions = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setContentView(dialogView);
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void addMenuItem(int icon, int title, Runnable listener) {
        addMenuItem(icon, title, listener, "");
    }

    public void addMenuItem(int icon, int title, Runnable listener, String description) {
        menuIcons.add(icon);
        menuTitles.add(title);
        menuListeners.add(listener);
        itemsEnabled.add(true);
        menuDescriptions.add(description);
    }

    public void setMenuItemEnabled(int index, boolean enabled) {
        itemsEnabled.set(index, enabled);
    }

    public void showMenu() {
        recyclerView.setAdapter(new MenuAdapter());
        show();
    }

    private class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(
                    getLayoutInflater().inflate(R.layout.menu_item, parent, false)
            ) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();

            ImageView icon = view.findViewById(R.id.icon);
            TextView title = view.findViewById(R.id.title);
            TextView description = view.findViewById(R.id.description);

            icon.setImageResource(selection == -1 ? menuIcons.get(pos)
                    : selection == pos ? R.drawable.baseline_radio_button_checked_24
                    : R.drawable.baseline_radio_button_unchecked_24);
            title.setText(menuTitles.get(pos));
            description.setVisibility(menuDescriptions.get(pos).isEmpty() ? View.GONE : View.VISIBLE);
            description.setText(menuDescriptions.get(pos));
            view.setOnClickListener(v -> {
                menuListeners.get(pos).run();
                dismiss();
            });
            view.setAlpha(itemsEnabled.get(pos) ? 1 : 0.5F);
            view.setClickable(itemsEnabled.get(pos));
        }

        @Override
        public int getItemCount() {
            return menuIcons.size();
        }
    }
}
