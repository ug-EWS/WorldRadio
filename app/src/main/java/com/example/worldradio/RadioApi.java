package com.example.worldradio;

import java.util.List;
import java.util.Locale;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.FieldName;
import de.sfuhrm.radiobrowser4j.Limit;
import de.sfuhrm.radiobrowser4j.ListParameter;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.SearchMode;
import de.sfuhrm.radiobrowser4j.Station;

public class RadioApi {
    public RadioStationCallback rsc;
    public LopCallback loc;
    private final RadioBrowser rb;

    RadioApi(RadioStationCallback _rsc) {
        rsc = _rsc;
        rb = getRadioBrowser();
    }

    RadioApi(LopCallback _loc) {
        loc = _loc;
        rb = getRadioBrowser();
    }

    public void getRadioStations(String name) {
        if (rsc != null)
            new Thread(() -> {
                Playlist resultPlaylist = new Playlist();
                    try {
                        rb.listStationsBy(SearchMode.BYNAME, name, ListParameter.create().order(FieldName.NAME))
                                .forEach(station -> resultPlaylist.addRadioStationToEnd(
                                        new RadioStation(station.getName(),
                                                station.getStationUUID().toString(),
                                                station.getUrlResolved(),
                                                station.getFavicon(),
                                                station.getHls())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                rsc.onGotRadioStations(resultPlaylist);
            }).start();
    }

    public void getRadioStations(Playlist playlist) {
        if (loc != null)
            new Thread(() -> {
                try {
                    int type = playlist.type;
                    String name = type == 2 ? playlist.countryCode : playlist.title;
                    if (type == 1 || type == 6) {
                        List<Station> list = type == 1 ? rb.listTopClickStations(Limit.of(20)) : rb.listTopVoteStations(Limit.of(20));
                        list.forEach(station -> playlist.addRadioStationToEnd(
                                    new RadioStation(station.getName(),
                                            station.getStationUUID().toString(),
                                            station.getUrlResolved(),
                                            station.getFavicon(),
                                            station.getHls()
                                    )));
                    }
                    else {
                        SearchMode sm;
                        switch (type) {
                            case 2:
                                sm = SearchMode.BYCOUNTRYCODEEXACT;
                                break;
                            case 3:
                                sm = SearchMode.BYLANGUAGEEXACT;
                                break;
                            default:
                                sm = SearchMode.BYTAGEXACT;
                                break;
                        }
                        rb.listStationsBy(sm, name, ListParameter.create().order(FieldName.NAME))
                                .forEach(station -> playlist.addRadioStationToEnd(
                                        new RadioStation(station.getName(),
                                                station.getStationUUID().toString(),
                                                station.getUrlResolved(),
                                                station.getFavicon(),
                                                station.getHls()
                                        )));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                loc.onGotRadioStations(playlist);
            }).start();
    }

    public void getListOfPlaylists(int type) {
        if (loc != null)
            new Thread(() -> {
                ListOfPlaylists lop = new ListOfPlaylists();
                switch (type) {
                    case 1:
                        lop.addPlaylist(new Playlist("En çok beğeni alanlar", 6, 20, null));
                        lop.addPlaylist(new Playlist("Dünya Top20", 1, 20, null));
                        break;
                    case 2:
                        rb.listCountryCodes().forEach(((s, integer) ->
                                lop.addPlaylist(new Playlist(new Locale("", s).getDisplayCountry(), 2, integer, s))
                        ));
                        lop.sortPlaylists();
                        break;
                    case 3:
                        rb.listLanguages().forEach(((s, integer) ->
                                lop.addPlaylist(new Playlist(s, 3, integer, null))
                        ));
                        lop.sortPlaylists();
                        break;
                    case 4:
                        rb.listTags().forEach(((s, integer) ->
                                lop.addPlaylist(new Playlist(s, 4, integer, null))
                                ));
                        lop.sortPlaylists();
                        break;
                    default:
                        break;
                }
                loc.onGotListOfPlaylists(lop, type);
            }).start();
    }

    private RadioBrowser getRadioBrowser() {
        return new RadioBrowser(ConnectionParams.builder()
                .apiUrl("https://de1.api.radio-browser.info/")
                .userAgent("Demo agent/1.0")
                .timeout(5000)
                .build());
    }

    public interface RadioStationCallback {
        void onGotRadioStations(Playlist radioStations);
    }

    public interface LopCallback {
        void onGotListOfPlaylists(ListOfPlaylists listOfPlaylists, int _type);
        void onGotRadioStations(Playlist radioStations);
    }
}
