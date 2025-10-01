package com.example.worldradio;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListOfPlaylists {
    ArrayList<Playlist> playlists;

    ListOfPlaylists() {
        playlists = new ArrayList<>();
    }

    ListOfPlaylists(String jsonString) {
        fromJsonString(jsonString);
    }

    public void fromJsonString(String jsonString) {
        try {
            fromJSONArray(new JSONArray(jsonString));
        } catch (JSONException e) {
            playlists = new ArrayList<>();
        }
    }

    private void fromJSONArray(JSONArray jsonArray) {
        playlists = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                playlists.add(new Playlist(jsonObject));
            } catch (JSONException e) {
                try {
                    String json = jsonArray.getString(i);
                    playlists.add(new Playlist(json));
                } catch (JSONException f) {
                    playlists.add(new Playlist("{}"));
                }
            }
        }
    }

    public void addPlaylist(Playlist p) {
        addPlaylistTo(p, 0);
    }

    public void addPlaylistTo(Playlist p, int to) {
        if (playlists.size() < to) to = 0;
        playlists.add(to, p);
    }

    public Playlist getPlaylistAt(int index) {
        return playlists.get(index);
    }

    public int getPlaylistById(String id) {
        for (int i = 0; i < playlists.size(); i++)
            if (playlists.get(i).playlistId.equals(id)) return i;
        return 0;
    }

    public int getIndexOf(Playlist playlist) {
        return playlists.indexOf(playlist);
    }

    private JSONArray toJSONArray(boolean forTempJson) {
        JSONArray jsonArray = new JSONArray();
        for (Playlist i : playlists) {
            jsonArray.put(i.toJSONObject(forTempJson));
        }
        return jsonArray;
    }

    public String toJsonString(boolean forTempJson) {
        return toJSONArray(forTempJson).toString();
    }

    public int getLength() {
        return playlists.size();
    }

    public void removePlaylist(int index) {
        playlists.remove(index);
    }

    public void removePlaylists(@NonNull ArrayList<Integer> indexes) {
        indexes.sort(Comparator.reverseOrder());
        for (Integer i : indexes) playlists.remove((int) i);
    }

    public void movePlaylistDep(int from, int to) {
        if (from == to) return;
        if (from < to) to--;
        Playlist playlistToMove = playlists.get(from);
        playlists.remove(from);
        if (playlists.isEmpty()) playlists.add(playlistToMove); else playlists.add(to, playlistToMove);
    }

    public void movePlaylist(int from, int to) {
        if (from < to) for (int i = from; i < to; i++) Collections.swap(playlists, i, i + 1);
        else for (int i = from; i > to; i--) Collections.swap(playlists, i, i - 1);
    }

    public String mergePlaylists(@NonNull ArrayList<Integer> indexes) {
        indexes.sort(Comparator.naturalOrder());
        Playlist basePlaylist = playlists.get(indexes.get(0));
        for (int i = indexes.size() - 1; i > 0; i--) {
            Playlist playlist = playlists.get(indexes.get(i));
            for (int j = playlist.getLength() - 1; j >= 0; j--) {
                RadioStation station = playlist.getRadioStationAt(j);
                if (!basePlaylist.contains(station))
                    basePlaylist.addRadioStation(station);
            }
            removePlaylist(indexes.get(i));
        }
        return basePlaylist.title;
    }

    public void sortPlaylists() {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        playlists.sort(Comparator.comparing(playlist -> playlist.title, collator::compare));
    }

    public boolean isEmpty() {
        return playlists.isEmpty();
    }
}