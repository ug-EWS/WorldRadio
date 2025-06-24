package com.example.worldradio;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Playlist {
    public String title, countryCode;
    public int icon, type, length;
    private ArrayList<RadioStation> radioStations;

    Playlist() {
        title = "";
        icon = 0;
        type = 0;
        length = 0;
        radioStations = new ArrayList<>();
    }

    Playlist(String _title) {
        title = _title;
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
    }

    Playlist(String _title, int _type, int _length, String _countryCode) {
        title = _title;
        icon = 0;
        type = _type;
        if (type == 2) countryCode = _countryCode;
        length = _length;
        radioStations = new ArrayList<>();
    }

    public Playlist fromJson (String _json) {
        HashMap<String, Object> map = Json.toMap(_json);
        title = (String) map.get("title");
        icon = map.containsKey("icon") ? Integer.parseInt((String) map.get("icon")) : R.drawable.baseline_featured_play_list_24;
        ArrayList<String> sourceList = Json.toList((String) map.get("radioStations"));
        RadioStation ytv;
        radioStations = new ArrayList<>();
        for (String i: sourceList) {
            ytv = new RadioStation().fromJson(i);
            radioStations.add(ytv);
        }

        return this;
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

    public RadioStation getRadioStationAt(int index) {
        return radioStations.get(index);
    }

    public int getIndexOf(RadioStation video) {
        return radioStations.indexOf(video);
    }

    public boolean isEmpty() {return radioStations.isEmpty();}

    public int getLength() {return length == 0 ? radioStations.size() : length;}

    public String getJson(){
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        for (RadioStation i : radioStations) list.add(i.getJson());
        map.put("title", title);
        map.put("icon", String.valueOf(icon));
        map.put("radioStations", Json.valueOf(list));
        return Json.valueOf(map);
    }
}