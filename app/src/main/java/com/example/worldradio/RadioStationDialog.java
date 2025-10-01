package com.example.worldradio;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

class RadioStationDialog extends BottomSheetDialog implements RadioApi.RadioStationCallback {
    MainActivity activity;
    View dialogView;
    ImageView cancelButton;
    EditText search;
    RecyclerView searchResults;
    SearchResultsAdapter searchResultsAdapter;
    Playlist resultPlaylist;
    TextView info;
    LinearLayout queryWarning;
    ImageView searchButton;
    int whereToAdd;

    RadioStationDialog(MainActivity _activity) {
        super(_activity, R.style.BottomSheetDialogTheme);
        activity = _activity;
        dialogView = getLayoutInflater().inflate(R.layout.add_video, null);
        cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> cancel());
        search = dialogView.findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchButton.setVisibility(s.length() < 3 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == IME_ACTION_GO) search();
            return true;
        });
        searchButton = dialogView.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> search());
        searchResults = dialogView.findViewById(R.id.searchResults);
        searchResults.setLayoutManager(new LinearLayoutManager(activity));
        info = dialogView.findViewById(R.id.goToWebsite);
        queryWarning = dialogView.findViewById(R.id.warning);
        setOnDismissListener(dialog1 -> {
            queryWarning.setVisibility(View.GONE);
            OnlinePlaylistsUtils.hideKeyboard(activity, search);
        });
        setContentView(dialogView);
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showDialog() {
        showDialog(0);
    }

    public void showDialog(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        show();
        OnlinePlaylistsUtils.showKeyboard(activity, search);
    }

    private void search() {
        String query = search.getText().toString();
        if (query.length() < 3) {
            queryWarning.setVisibility(View.VISIBLE);
        } else {
            queryWarning.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(search.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            searchResults.setVisibility(View.GONE);
            if (OnlinePlaylistsUtils.isConnected(activity)) {
                info.setText(R.string.loading);
                new RadioApi(this).getRadioStations(formatQuery(query));
            } else {
                info.setText(activity.getString(R.string.check_internet_connection));
            }
            info.setVisibility(View.VISIBLE);
        }
    }

    public void onGotRadioStations(Playlist _radioStations) {
        resultPlaylist = _radioStations;
        activity.runOnUiThread(() -> {
            if (resultPlaylist.isEmpty()) {
                info.setText(R.string.no_results_found);
            } else {
                searchResults.setVisibility(View.VISIBLE);
                info.setVisibility(View.GONE);
                searchResultsAdapter = new SearchResultsAdapter(this);
                searchResults.setAdapter(searchResultsAdapter);
            }
        });
    }

    private String formatQuery(String query) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return query.replace("+", "%20");
    }
}