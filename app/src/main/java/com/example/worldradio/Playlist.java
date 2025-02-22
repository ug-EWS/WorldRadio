package com.example.worldradio;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Playlist {
    public String title;
    public int icon;
    private ArrayList<RadioStation> radioStations;

    Playlist() {
        title = "";
        icon = 0;
        radioStations = new ArrayList<>();
    }

    Playlist(String _title) {
        title = _title;
        icon = 0;
        radioStations = new ArrayList<>();
    }

    Playlist(String _title, int _icon) {
        title = _title;
        icon = _icon;
        radioStations = new ArrayList<>();
    }

    public Playlist fromJson (String _json) {
        radioStations = new ArrayList<>();
        HashMap<String, Object> map = Json.toMap(_json);
        ArrayList<String> sourceList = Json.toList(map.get("radioStations").toString());
        title = map.get("title").toString();
        icon = map.containsKey("icon") ? Integer.parseInt(map.get("icon").toString()) : R.drawable.baseline_featured_play_list_24;
        RadioStation ytv;

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
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(radioStations, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(radioStations, i, i - 1);
            }
        }
    }

    public RadioStation getRadioStationAt(int index) {
        return radioStations.get(index);
    }

    public int getIndexOf(RadioStation video) {
        return radioStations.indexOf(video);
    }

    public boolean isEmpty() {return radioStations.isEmpty();}

    public int getLength() {
        return radioStations.size();
    }

    public String getJson(){
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        for (RadioStation i : radioStations) {
            list.add(i.getJson());
        }
        map.put("title", title);
        map.put("icon", String.valueOf(icon));
        map.put("radioStations", Json.valueOf(list));
        return Json.valueOf(map);
    }
}