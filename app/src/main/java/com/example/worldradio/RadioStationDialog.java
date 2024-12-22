package com.example.worldradio;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import de.sfuhrm.radiobrowser4j.FieldName;
import de.sfuhrm.radiobrowser4j.ListParameter;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.SearchMode;

class RadioStationDialog {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    View dialogView;
    EditText search;
    RecyclerView searchResults;
    SearchResultsAdapter searchResultsAdapter;
    Playlist resultPlaylist;
    RadioBrowser radioBrowser;
    TextView noResults;
    Button cancelButton;
    Spinner spinner;
    TextView loading;
    SearchMode searchMode;
    boolean searchByCountry;
    int whereToAdd;

    RadioStationDialog(MainActivity _activity) {
        activity = _activity;
        radioBrowser = activity.radioBrowser;

        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle(activity.getString(R.string.add_video));
        dialogView = activity.getLayoutInflater().inflate(R.layout.add_video, null);
        search = dialogView.findViewById(R.id.search);
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == IME_ACTION_GO) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(search.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                resultPlaylist = new Playlist();
                searchResults.setVisibility(View.GONE);
                noResults.setVisibility(View.GONE);
                loading.setVisibility(View.VISIBLE);
                if (radioBrowser != null) {
                    Thread networkThread = new Thread(() -> {
                        SearchMode[] searchModes = {SearchMode.BYNAME, SearchMode.BYCOUNTRYCODEEXACT, SearchMode.BYLANGUAGE, SearchMode.BYTAG, SearchMode.BYSTATE};
                        int position = spinner.getSelectedItemPosition();
                        searchMode = searchModes[position];
                        searchByCountry = position == 1;
                        String query;
                        if (searchByCountry) {
                            String countryName = search.getText().toString().toLowerCase();
                            query = "";
                            for (String i : Locale.getISOCountries()) {
                                Locale locale = new Locale("", i);
                                if (locale.getDisplayCountry().toLowerCase().contains(countryName)) {
                                    query = i;
                                    break;
                                }
                            }
                        } else {
                            query = search.getText().toString();
                            try {
                                query = URLEncoder.encode(query, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                            query = query.replace("+", "%20");
                        }
                        if(!query.isEmpty()) {
                            try {
                                radioBrowser.listStationsBy(searchMode, query, ListParameter.create().order(FieldName.NAME))
                                        .forEach(station -> {
                                            resultPlaylist.addRadioStationToEnd(
                                                    new RadioStation(station.getName(),
                                                            station.getStationUUID().toString(),
                                                            station.getUrlResolved(),
                                                            station.getFavicon(),
                                                            station.getHls()));
                                        });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        activity.runOnUiThread(() -> {
                            if (resultPlaylist.isEmpty()) {
                                loading.setVisibility(View.GONE);
                                noResults.setVisibility(View.VISIBLE);
                            } else {
                                searchResults.setVisibility(View.VISIBLE);
                                loading.setVisibility(View.GONE);
                                searchResultsAdapter.notifyDataSetChanged();
                            }
                        });
                    });
                    networkThread.start();
                }

            }
            return true;
        });
        searchResults = dialogView.findViewById(R.id.searchResults);
        searchResultsAdapter = new SearchResultsAdapter(this);
        searchResults.setLayoutManager(new LinearLayoutManager(activity));
        searchResults.setAdapter(searchResultsAdapter);
        noResults = dialogView.findViewById(R.id.noResults);
        cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        spinner = dialogView.findViewById(R.id.spinner);
        loading = dialogView.findViewById(R.id.loading);
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
}
