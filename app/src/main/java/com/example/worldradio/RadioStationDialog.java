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
import android.widget.LinearLayout;
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
    TextView info;
    LinearLayout queryWarning;
    ImageView searchButton;
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
        info = dialogView.findViewById(R.id.info);
        queryWarning = dialogView.findViewById(R.id.warning);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
        builder.setOnDismissListener(dialog1 -> {
            queryWarning.setVisibility(View.GONE);
            OnlinePlaylistsUtils.hideKeyboard(activity, search);
        }
        );
        builder.setView(dialogView);
        dialog = builder.create();
    }

    public void show() {
        show(0);
    }

    public void show(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        dialog.show();
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