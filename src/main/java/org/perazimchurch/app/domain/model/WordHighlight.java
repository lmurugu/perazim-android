package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/** A word or character-range highlight inside a verse body: [start, end) offsets and a colour name. */
public class WordHighlight implements Serializable {

    private final int start;
    private final int end;
    private final String colour;

    public WordHighlight(int start, int end, String colour) {
        this.start = start;
        this.end = end;
        this.colour = colour;
    }

    public int getStart() { return start; }

    public int getEnd() { return end; }

    public String getColour() { return colour; }

    /** True when this range shares at least one character with [start, end). */
    public boolean overlaps(int otherStart, int otherEnd) {
        return this.start < otherEnd && otherStart < this.end;
    }
}
