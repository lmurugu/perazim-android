package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Room entity for table "bible_topic_verses": a verse saved under a topic label such as "healing".
 * Topic links are user data and are stored per verse position, shared by every translation.
 */
@Entity(
    tableName = "bible_topic_verses",
    primaryKeys = {"topic", "bookNumber", "chapterNumber", "verseNumber"}
)
public class BibleTopicVerseEntity {

    @NonNull
    private String topic;
    private int bookNumber;
    private int chapterNumber;
    private int verseNumber;

    public BibleTopicVerseEntity(@NonNull String topic, int bookNumber, int chapterNumber, int verseNumber) {
        this.topic = topic;
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
    }

    @NonNull
    public String getTopic() { return topic; }
    public void setTopic(@NonNull String topic) { this.topic = topic; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseNumber() { return verseNumber; }
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber; }
}
