package com.example.worldradio;

import java.util.HashMap;

public class RadioStation {
    public String title;
    public String id;
    public String url;
    public String faviconUrl;
    public String hls;

    RadioStation() {
        title = "";
        id = "";
        url = "";
        faviconUrl = "";
        hls = "";
    }

    RadioStation(String _title, String _id, String _url, String _faviconUrl, String _hls) {
        title = _title;
        id = _id;
        url = _url;
        faviconUrl = _faviconUrl;
        hls = _hls;
    }

    public RadioStation fromJson(String _json) {
        HashMap<String, Object> map = Json.toMap(_json);
        title = map.get("title").toString();
        id = map.get("id").toString();
        url = map.get("url").toString();
        faviconUrl = map.get("faviconUrl").toString();
        if (map.containsKey("hls")) hls = map.get("hls").toString();
        return this;
    }

    public String getJson() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("id", id);
        map.put("url", url);
        map.put("faviconUrl", faviconUrl);
        map.put("hls", hls);
        return Json.valueOf(map);
    }
}
