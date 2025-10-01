package com.example.worldradio;

import org.json.JSONException;
import org.json.JSONObject;

public class RadioStation {
    public String title;
    public String id;
    public String url;
    public String faviconUrl;
    public String homepage;
    public String codec;
    public int bitrate;

    RadioStation(String _title, String _id, String _url, String _faviconUrl, String _homepage, String _codec, int _bitrate) {
        title = _title;
        id = _id;
        url = _url;
        faviconUrl = _faviconUrl;
        homepage = _homepage;
        codec = _codec;
        bitrate = _bitrate;
    }

    RadioStation(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    RadioStation(String jsonString) {
        try {
            fromJSONObject(new JSONObject(jsonString));
        } catch (JSONException e) {
            title = "";
            id = "";
            url = "";
            faviconUrl = "";
            homepage = "";
        }
    }

    private void fromJSONObject(JSONObject jsonObject) {
        title = jsonObject.optString("title");
        id = jsonObject.optString("id");
        url = jsonObject.optString("url");
        faviconUrl = jsonObject.optString("faviconUrl");
        homepage = jsonObject.optString("homepage");
        codec = jsonObject.optString("codec");
        bitrate = jsonObject.optInt("bitrate");
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("title", title)
                    .put("id", id)
                    .put("url", url)
                    .put("faviconUrl", faviconUrl)
                    .put("homepage", homepage)
                    .put("codec", codec)
                    .put("bitrate", bitrate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
