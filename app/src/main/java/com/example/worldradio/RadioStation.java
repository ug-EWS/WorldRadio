package com.example.worldradio;

import java.util.HashMap;

public class RadioStation {
    public String title;
    public String id;
    public String url;
    public String faviconUrl;
    public String hls;
    public String homepage;
    public String clickTrend;

    RadioStation() {
        title = "";
        id = "";
        url = "";
        faviconUrl = "";
        hls = "";
        homepage = "";
        clickTrend = "";
    }

    RadioStation(String _title, String _id, String _url, String _faviconUrl, String _hls, String _homepage, String _clickTrend) {
        title = _title;
        id = _id;
        url = _url;
        faviconUrl = _faviconUrl;
        hls = _hls;
        homepage = _homepage;
        clickTrend = _clickTrend;
    }

    public RadioStation fromJson(String _json, boolean isTempJson) {
        HashMap<String, Object> map = Json.toMap(_json);
        title = map.get("title").toString();
        id = map.get("id").toString();
        url = map.get("url").toString();
        faviconUrl = map.get("faviconUrl").toString();
        if (map.containsKey("hls")) hls = map.get("hls").toString();
        if (map.containsKey("homepage")) homepage = map.get("homepage").toString();
        if (isTempJson && map.containsKey("clickTrend")) clickTrend = map.get("clickTrend").toString();
        return this;
    }

    public String getJson(boolean isTempJson) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("id", id);
        map.put("url", url);
        map.put("faviconUrl", faviconUrl);
        map.put("hls", hls);
        map.put("homepage", homepage);
        if (isTempJson) map.put("clickTrend", clickTrend);
        return Json.valueOf(map);
    }
}
