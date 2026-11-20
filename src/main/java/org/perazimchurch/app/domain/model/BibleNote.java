package org.perazimchurch.app.domain.model;

import java.util.ArrayList;
import java.util.List;

/** A study note with the verses it links to and the topics it belongs to. */
public class BibleNote {
    private final long id;
    private final String body;
    private final long updatedMillis;
    private final List<VerseLocation> links;
    private final List<String> topics;

    public BibleNote(long id, String body, long updatedMillis, List<VerseLocation> links, List<String> topics) {
        this.id = id;
        this.body = body;
        this.updatedMillis = updatedMillis;
        this.links = new ArrayList<>(links);
        this.topics = new ArrayList<>(topics);
    }

    public long getId() { return id; }
    /** Note body as HTML (rich text), or plain text for notes written before formatting existed. */
    public String getBody() { return body; }
    public long getUpdatedMillis() { return updatedMillis; }
    public List<VerseLocation> getLinks() { return links; }
    public List<String> getTopics() { return topics; }
}
