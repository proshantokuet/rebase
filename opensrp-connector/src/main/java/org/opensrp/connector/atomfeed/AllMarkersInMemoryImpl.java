package org.opensrp.connector.atomfeed;
import org.ict4h.atomfeed.client.domain.Marker;
import org.ict4h.atomfeed.client.repository.AllMarkers;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.HashMap;

//@Repository
public class AllMarkersInMemoryImpl implements AllMarkers {

    private final HashMap<URI,Marker> map;

    public AllMarkersInMemoryImpl() {
        map = new HashMap<URI,Marker>();
    }

    @Override
    public Marker get(URI feedUri) {
        return map.get(feedUri);
    }

    @Override
    public void put(URI feedUri, String entryId, URI entryFeedUri) {
        map.put(feedUri, new Marker(feedUri, entryId, entryFeedUri));
    }

}