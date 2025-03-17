package com.example.worldradio;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.FieldName;
import de.sfuhrm.radiobrowser4j.ListParameter;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.SearchMode;

class RadioStationDialog implements RadioApi.RadioStationCallback {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    View dialogView;
    EditText search;
    RecyclerView searchResults;
    SearchResultsAdapter searchResultsAdapter;
    Playlist resultPlaylist;
    Button cancelButton;
    Spinner spinner;
    TextView info;
    ImageView searchButton;
    SearchMode searchMode;
    boolean searchByCountry;
    int whereToAdd;

    RadioStationDialog(MainActivity _activity) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle(activity.getString(R.string.add_video));
        dialogView = activity.getLayoutInflater().inflate(R.layout.add_video, null);
        search = dialogView.findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchButton.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
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
        cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        info = dialogView.findViewById(R.id.info);
        builder.setView(dialogView);
        dialog = builder.create();
    }

    public void show() {
        show(0);
    }

    public void show(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        dialog.show();
    }

    private void search() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(search.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        searchResults.setVisibility(View.GONE);
        if (OnlinePlaylistsUtils.isConnected(activity)) {
            info.setText(R.string.loading);
            new RadioApi(this).getRadioStations(formatQuery(search.getText().toString()));
        } else {
            info.setText("Lütfen internet bağlantınızı kontrol edin.");
        }
        info.setVisibility(View.VISIBLE);
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