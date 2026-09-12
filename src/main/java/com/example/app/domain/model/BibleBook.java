package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Book of the Bible.
 * Compliant with Guidebook §34.
 */
public class BibleBook implements Serializable {

    private String id; // e.g. "GEN", "2SA", "MAT"
    private String name; // e.g. "Genesis", "2 Samuel", "Matthew"
    private int bookNumber; // 1 to 66
    private String testament; // "OLD", "NEW"
    private int chapterCount;

    public BibleBook() {
    }

    public BibleBook(String id, String name, int bookNumber, String testament, int chapterCount) {
        this.id = id;
        this.name = name;
        this.bookNumber = bookNumber;
        this.testament = testament;
        this.chapterCount = chapterCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public String getTestament() { return testament; }
    public void setTestament(String testament) { this.testament = testament; }

    public int getChapterCount() { return chapterCount; }
    public void setChapterCount(int chapterCount) { this.chapterCount = chapterCount; }
}
