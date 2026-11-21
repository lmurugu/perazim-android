package org.perazimchurch.app.data.local.text;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The separate Bible text database. It is a plain SQLite file because it holds only public Bible text,
 * which keeps the private encrypted database small so the app opens quickly.
 */
@Database(entities = {BibleTextVerseEntity.class}, version = 1, exportSchema = false)
public abstract class BibleTextDatabase extends RoomDatabase {

    public static final String NAME = "bible_text.db";
    private static volatile BibleTextDatabase instance;

    public abstract BibleTextDao textDao();

    public static BibleTextDatabase get(Context context) {
        if (instance == null) {
            synchronized (BibleTextDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), BibleTextDatabase.class, NAME)
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .build();
                }
            }
        }
        return instance;
    }
}
