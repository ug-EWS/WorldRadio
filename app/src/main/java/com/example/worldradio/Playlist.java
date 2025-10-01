package com.example.worldradio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class Playlist {
    public String title, countryCode, playlistId;
    public int icon, type, length, sortBy;
    private ArrayList<RadioStation> radioStations;

    private static long currentMillis;
    private static int idCounter;

    public static final int TITLE = 0;
    public static final int BITRATE = 1;
    public static final int CODEC = 2;

    Playlist() {
        title = "";
        icon = 0;
        type = 0;
        length = 0;
        radioStations = new ArrayList<>();
    }

    Playlist(String _title, int _icon) {
        title = _title;
        icon = _icon;
        type = 0;
        length = 0;
        radioStations = new ArrayList<>();
        playlistId = generateId();
    }

    Playlist(String _title, int _type, int _length, String _countryCode) {
        title = _title;
        icon = 0;
        type = _type;
        if (type == 2) countryCode = _countryCode;
        length = _length;
        radioStations = new ArrayList<>();
    }

    Playlist(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    Playlist(String jsonString) {
        try {
            fromJSONObject(new JSONObject(jsonString));
        } catch (JSONException e) {
            title = "";
            icon = 0;
            radioStations = new ArrayList<>();
            countryCode = "";
            type = 0;
            length = 0;
            playlistId = generateId();
        }
    }

    private void fromJSONObject(JSONObject jsonObject) {
        title = jsonObject.optString("title");
        icon = jsonObject.optInt("icon", 0);
        playlistId = jsonObject.optString("playlistId", generateId());
        countryCode = jsonObject.optString("countryCode", "");
        type = jsonObject.optInt("type", 0);
        length = jsonObject.optInt("length", 0);
        sortBy = jsonObject.optInt("sortBy", 0);
        radioStations = new ArrayList<>();
        JSONArray radioStationsArray;
        try {
            radioStationsArray = jsonObject.getJSONArray("radioStations");
        } catch (JSONException e) {
            try {
                radioStationsArray = new JSONArray(jsonObject.getString("radioStations"));
            } catch (JSONException f) {
                radioStationsArray = new JSONArray();
            }
        }
        for (int i = 0; i < radioStationsArray.length(); i++) {
            try {
                JSONObject object = radioStationsArray.getJSONObject(i);
                radioStations.add(new RadioStation(object));
            } catch (JSONException e) {
                try {
                    String json = radioStationsArray.getString(i);
                    radioStations.add(new RadioStation(json));
                } catch (JSONException f) {
                    radioStations.add(new RadioStation("{}"));
                }
            }
        }
    }

    private static String generateId() {
        Calendar calendar = Calendar.getInstance();
        long millis = calendar.getTimeInMillis();
        if (millis == currentMillis) {
            idCounter++;
            return String.valueOf(millis).concat("-").concat(String.valueOf(idCounter));
        } else {
            currentMillis = millis;
            idCounter = 0;
            return String.valueOf(millis);
        }
    }

    public void addRadioStation(RadioStation _station) {
        addRadioStationTo(_station, 0);
    }

    public void addRadioStationToEnd(RadioStation _station) {
        radioStations.add(_station);
    }

    public void addRadioStationTo(RadioStation _station, int to) {
        if (to > radioStations.size()) to = 0;
        radioStations.add(to, _station);
    }

    public boolean contains(RadioStation _station) {
        boolean _contains = false;
        String id = _station.id;
        for (RadioStation i : radioStations) {
            if (id.equals(i.id)) {
                _contains = true;
                break;
            }
        }
        return _contains;
    }

    public void removeRadioStation(int index) {
        radioStations.remove(index);
    }

    public void removeRadioStations(ArrayList<Integer> indexes) {
        indexes.sort(Comparator.reverseOrder());
        for (Integer i : indexes) radioStations.remove((int) i);
    }

    public void moveVideoDep(int from, int to) {
        if (from == to) return;
        if (from < to) to--;
        RadioStation videoToMove = radioStations.get(from);
        radioStations.remove(from);
        if (radioStations.isEmpty()) radioStations.add(videoToMove); else radioStations.add(to, videoToMove);
    }

    public void moveRadioStation(int from, int to) {
        if (from < to) for (int i = from; i < to; i++) Collections.swap(radioStations, i, i + 1);
        else for (int i = from; i > to; i--) Collections.swap(radioStations, i, i - 1);
    }

    public void sort(int _sortBy) {
        sortBy = _sortBy;
        if (sortBy == TITLE)
            radioStations.sort(Comparator.comparing(station -> station.title));
        if (sortBy == BITRATE)
            radioStations.sort(Comparator.comparing(station -> station.bitrate, Comparator.reverseOrder()));
        if (sortBy == CODEC)
            radioStations.sort(Comparator.comparing(station -> station.codec));
    }

    public RadioStation getRadioStationAt(int index) {
        return radioStations.get(index);
    }

    public int getIndexOf(RadioStation video) {
        return radioStations.indexOf(video);
    }

    public boolean isEmpty() {return radioStations.isEmpty();}

    public int getLength() {return radioStations.isEmpty() ? length : radioStations.size();}

    public JSONObject toJSONObject(boolean forTempJson) {
        JSONArray radioStationsArray = new JSONArray();
        for (RadioStation i : radioStations) radioStationsArray.put(i.toJSONObject());
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("title", title)
                    .put("icon", icon)
                    .put("radioStations", radioStationsArray);
            if (forTempJson) {
                jsonObject.put("countryCode", countryCode)
                        .put("type", type)
                        .put("length", length)
                        .put("sortBy", sortBy);
            } else jsonObject.put("playlistId", playlistId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String toJsonString(boolean forTempJson) {
        return toJSONObject(forTempJson).toString();
    }
}