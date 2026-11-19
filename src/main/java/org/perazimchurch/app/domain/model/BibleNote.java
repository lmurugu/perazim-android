package org.perazimchurch.app.domain.model;

import java.util.ArrayList;
import java.util.List;

/** A study note with the verses it links to. */
public class BibleNote {
    private final long id;
    private final String body;
    private final long updatedMillis;
    private final List<VerseLocation> links;

    public BibleNote(long id, String body, long updatedMillis, List<VerseLocation> links) {
        this.id = id;
        this.body = body;
        this.updatedMillis = updatedMillis;
        this.links = new ArrayList<>(links);
    }

    public long getId() { return id; }
    public String getBody() { return body; }
    public long getUpdatedMillis() { return updatedMillis; }
    public List<VerseLocation> getLinks() { return links; }
}
