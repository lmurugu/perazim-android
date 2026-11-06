package org.perazimchurch.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plays audio files the user selects from this phone, with persisted SAF access. */
public class LocalBibleAudioActivity extends Activity {
    private static final int REQUEST_AUDIO = 4817;
    private static final int REQUEST_AUDIO_PERMISSION = 4818;
    private static final long MAX_FALLBACK_AUDIO_BYTES = 25L * 1024L * 1024L;
    private static final String PREFS = "local_bible_audio";
    private static final String KEY_ITEMS = "items";
    private static final String[] BOOKS = {
            "Jude", "Philemon", "2 John", "3 John", "Obadiah", "Nahum", "Habakkuk",
            "Zephaniah", "Haggai", "Malachi", "1 Peter", "2 Peter", "James", "1 John",
            "2 Timothy", "Titus", "Jonah", "Ruth", "Esther", "Lamentations", "Other"
    };

    private final List<AudioItem> items = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Uri playingUri;
    private boolean prepared;
    private String pendingBook;
    private boolean pendingPersistable;
    private TextView nowPlaying;
    private TextView timeLabel;
    private SeekBar seekBar;
    private Button playPause;
    private LinearLayout list;
    private final Runnable progressUpdate = new Runnable() {
        @Override public void run() {
            if (player != null && prepared && player.isPlaying()) {
                seekBar.setProgress(player.getCurrentPosition());
                timeLabel.setText(formatTime(player.getCurrentPosition()) + " / " + formatTime(player.getDuration()));
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pendingBook = savedInstanceState.getString("pending_book");
            pendingPersistable = savedInstanceState.getBoolean("pending_persistable", false);
        }
        loadItems();
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(0xFF2A0F33);

        TextView title = new TextView(this);
        title.setText("Bible audio on this phone");
        title.setTextSize(22);
        title.setTextColor(0xFFF5F0F7);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("Choose an audio file already on your phone and save it under a Bible book.");
        intro.setTextColor(0xFFB9A8C0);
        intro.setPadding(0, dp(6), 0, dp(14));
        root.addView(intro);

        Button add = new Button(this);
        add.setText("Add Bible audio from this phone");
        add.setOnClickListener(v -> chooseBook());
        root.addView(add);

        nowPlaying = new TextView(this);
        nowPlaying.setText("Nothing playing");
        nowPlaying.setTextColor(0xFFF5F0F7);
        nowPlaying.setPadding(0, dp(20), 0, dp(4));
        root.addView(nowPlaying);

        timeLabel = new TextView(this);
        timeLabel.setText("0:00 / 0:00");
        timeLabel.setTextColor(0xFFB9A8C0);
        root.addView(timeLabel);

        seekBar = new SeekBar(this);
        seekBar.setEnabled(false);
        root.addView(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && player != null && prepared) {
                    player.seekTo(progress);
                    timeLabel.setText(formatTime(progress) + " / " + formatTime(player.getDuration()));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        playPause = new Button(this);
        playPause.setText("Play");
        playPause.setOnClickListener(v -> togglePlayback());
        controls.addView(playPause);
        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(v -> stopPlayback());
        controls.addView(stop);
        root.addView(controls);

        TextView savedTitle = new TextView(this);
        savedTitle.setText("SAVED AUDIO");
        savedTitle.setTextColor(0xFFE17D2F);
        savedTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        savedTitle.setPadding(0, dp(20), 0, dp(8));
        root.addView(savedTitle);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView savedScroll = new android.widget.ScrollView(this);
        savedScroll.setFillViewport(false);
        savedScroll.addView(list);
        root.addView(savedScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        renderItems();

        setContentView(root);
    }

    private void chooseBook() {
        new AlertDialog.Builder(this)
                .setTitle("Which book is this audio for?")
                .setItems(BOOKS, (dialog, which) -> openAudioPicker(BOOKS[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAudioPicker(String book) {
        pendingBook = book;
        Intent openDocument = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        openDocument.addCategory(Intent.CATEGORY_OPENABLE);
        openDocument.setType("audio/*");
        openDocument.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (launchAudioPicker(openDocument, true)) return;

        Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
        getContent.addCategory(Intent.CATEGORY_OPENABLE);
        getContent.setType("audio/*");
        getContent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (launchAudioPicker(getContent, false)) return;

        requestMediaAudioPermission();
    }

    private boolean launchAudioPicker(Intent intent, boolean persistable) {
        try {
            pendingPersistable = persistable;
            startActivityForResult(intent, REQUEST_AUDIO);
            return true;
        } catch (android.content.ActivityNotFoundException e) {
            return false;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AUDIO) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingBook = null;
            pendingPersistable = false;
            renderItems();
            return;
        }
        Uri uri = data.getData();
        String book = pendingBook == null ? "Other" : pendingBook;
        String name = displayName(uri);
        if (pendingPersistable) {
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                Toast.makeText(this, "The selected file may need to be added again later.", Toast.LENGTH_LONG).show();
            }
        } else {
            uri = copySelectedAudio(uri, name);
            if (uri == null) {
                pendingBook = null;
                pendingPersistable = false;
                return;
            }
        }
        pendingPersistable = false;
        saveSelectedAudio(book, uri, name);
        pendingBook = null;
    }

    private void saveSelectedAudio(String book, Uri uri, String name) {
        for (AudioItem existing : items) {
            if (existing.uri.equals(uri.toString())) {
                existing.book = book;
                existing.name = name;
                saveItems();
                renderItems();
                play(existing);
                return;
            }
        }
        AudioItem item = new AudioItem(book, name, uri.toString());
        items.add(item);
        saveItems();
        renderItems();
        play(item);
    }

    private void requestMediaAudioPermission() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            showMediaAudioPicker();
        } else {
            String book = pendingBook == null ? "selected Bible book" : pendingBook;
            new AlertDialog.Builder(this).setTitle("Find " + book + " audio")
                    .setMessage("To list matching recordings already on this phone, Perazim needs audio access. It will show matching file names and sizes so you can choose one to play.")
                    .setPositiveButton("Continue", (dialog, which) -> requestPermissions(new String[]{permission}, REQUEST_AUDIO_PERMISSION))
                    .setNegativeButton("Cancel", (dialog, which) -> pendingBook = null).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_AUDIO_PERMISSION) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showMediaAudioPicker();
        } else {
            pendingBook = null;
            Toast.makeText(this, "Allow audio access to browse recordings on this phone.", Toast.LENGTH_LONG).show();
        }
    }

    private void showMediaAudioPicker() {
        List<AudioItem> matches = new ArrayList<>();
        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE};
        String selection = MediaStore.Audio.Media.SIZE + " > 0 AND " + MediaStore.Audio.Media.SIZE + " <= ?";
        String[] args = {String.valueOf(MAX_FALLBACK_AUDIO_BYTES)};
        try (Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args, MediaStore.Audio.Media.DISPLAY_NAME + " ASC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                String bookKey = normalize(pendingBook);
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameCol);
                    long size = cursor.getLong(sizeCol);
                    if ("Other".equals(pendingBook) || normalize(name).contains(bookKey)) {
                        Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                String.valueOf(cursor.getLong(idCol)));
                        matches.add(new AudioItem(pendingBook, name + "  ·  " + formatBytes(size), uri.toString()));
                    }
                }
            }
        } catch (SecurityException e) {
            pendingBook = null;
            Toast.makeText(this, "Could not access audio on this phone.", Toast.LENGTH_LONG).show();
            return;
        }
        if (matches.isEmpty()) {
            String book = pendingBook == null ? "this book" : pendingBook;
            pendingBook = null;
            new AlertDialog.Builder(this).setTitle("No matching audio found")
                    .setMessage("No audio for " + book + " under 25 MB was found on this phone.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] labels = new String[matches.size()];
        for (int i = 0; i < matches.size(); i++) labels[i] = matches.get(i).name;
        new AlertDialog.Builder(this).setTitle("Choose " + pendingBook + " audio")
                .setItems(labels, (dialog, which) -> {
                    AudioItem selected = matches.get(which);
                    saveSelectedAudio(selected.book, Uri.parse(selected.uri), selected.name);
                    pendingBook = null;
                }).setOnCancelListener(dialog -> pendingBook = null).show();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String formatBytes(long bytes) {
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f));
    }

    private Uri copySelectedAudio(Uri source, String displayName) {
        File folder = new File(getFilesDir(), "local_bible_audio");
        if (!folder.exists() && !folder.mkdirs()) {
            Toast.makeText(this, "Could not save the selected audio.", Toast.LENGTH_LONG).show();
            return null;
        }
        String cleanName = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File destination = new File(folder, System.currentTimeMillis() + "_" + cleanName);
        long total = 0;
        try (InputStream input = getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("Unable to open selected audio");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FALLBACK_AUDIO_BYTES) {
                    destination.delete();
                    Toast.makeText(this, "Choose an audio file under 25 MB.", Toast.LENGTH_LONG).show();
                    return null;
                }
                output.write(buffer, 0, read);
            }
            return Uri.fromFile(destination);
        } catch (IOException | SecurityException e) {
            destination.delete();
            Toast.makeText(this, "Could not save the selected audio.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (pendingBook != null) outState.putString("pending_book", pendingBook);
        outState.putBoolean("pending_persistable", pendingPersistable);
        super.onSaveInstanceState(outState);
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (Exception ignored) { }
        return "Selected audio";
    }

    private void renderItems() {
        if (list == null) return;
        list.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No audio saved yet.");
            empty.setTextColor(0xFFB9A8C0);
            list.addView(empty);
            return;
        }
        for (AudioItem item : new ArrayList<>(items)) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundColor(0xFF3D1749);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);
            TextView label = new TextView(this);
            label.setText(item.book + "  ·  " + item.name);
            label.setTextColor(0xFFF5F0F7);
            label.setTextSize(15);
            row.addView(label);
            LinearLayout buttons = new LinearLayout(this);
            Button play = new Button(this);
            play.setText(playingUri != null && playingUri.toString().equals(item.uri) && player != null && prepared && player.isPlaying() ? "Playing" : "Play");
            play.setOnClickListener(v -> play(item));
            buttons.addView(play);
            Button remove = new Button(this);
            remove.setText("Remove");
            remove.setOnClickListener(v -> remove(item));
            buttons.addView(remove);
            row.addView(buttons);
            list.addView(row);
        }
    }

    private void play(AudioItem item) {
        if (player != null && item.uri.equals(playingUri == null ? null : playingUri.toString())) {
            if (prepared && !player.isPlaying()) togglePlayback();
            return;
        }
        releasePlayer();
        try {
            player = new MediaPlayer();
            playingUri = Uri.parse(item.uri);
            player.setDataSource(this, Uri.parse(item.uri));
            player.setOnPreparedListener(mp -> {
                prepared = true;
                mp.start();
                playingUri = Uri.parse(item.uri);
                nowPlaying.setText(item.book + " · " + item.name);
                playPause.setText("Pause");
                seekBar.setMax(mp.getDuration());
                seekBar.setEnabled(true);
                timeLabel.setText("0:00 / " + formatTime(mp.getDuration()));
                renderItems();
                handler.removeCallbacks(progressUpdate);
                handler.post(progressUpdate);
            });
            player.setOnCompletionListener(mp -> {
                playPause.setText("Play");
                handler.removeCallbacks(progressUpdate);
                renderItems();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Could not play this audio file.", Toast.LENGTH_LONG).show();
                nowPlaying.setText("Could not play: " + item.name);
                playPause.setText("Play");
                releasePlayer();
                seekBar.setEnabled(false);
                renderItems();
                return true;
            });
            player.prepareAsync();
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            Toast.makeText(this, "This audio file is unavailable.", Toast.LENGTH_LONG).show();
            releasePlayer();
            nowPlaying.setText("Unavailable: " + item.name);
            playPause.setText("Play");
            seekBar.setEnabled(false);
            renderItems();
        }
    }

    private void togglePlayback() {
        if (player == null || !prepared) {
            Toast.makeText(this, "Choose saved audio to play.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            playPause.setText("Play");
            handler.removeCallbacks(progressUpdate);
        } else {
            player.start();
            playPause.setText("Pause");
            handler.post(progressUpdate);
        }
        renderItems();
    }

    private void stopPlayback() {
        if (player != null) {
            try { player.stop(); } catch (IllegalStateException ignored) { }
            releasePlayer();
        }
        nowPlaying.setText("Nothing playing");
        playPause.setText("Play");
        seekBar.setProgress(0);
        timeLabel.setText("0:00 / 0:00");
        renderItems();
    }

    private void remove(AudioItem item) {
        if (item.uri.equals(playingUri == null ? null : playingUri.toString())) stopPlayback();
        items.remove(item);
        Uri itemUri = Uri.parse(item.uri);
        if ("file".equals(itemUri.getScheme()) && itemUri.getPath() != null
                && itemUri.getPath().startsWith(new File(getFilesDir(), "local_bible_audio").getAbsolutePath())) {
            new File(itemUri.getPath()).delete();
        } else if (!"media".equals(itemUri.getAuthority())) {
            try { getContentResolver().releasePersistableUriPermission(itemUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (SecurityException ignored) { }
        }
        saveItems();
        renderItems();
    }

    private void releasePlayer() {
        handler.removeCallbacks(progressUpdate);
        prepared = false;
        if (player != null) {
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
        playingUri = null;
    }

    @Override protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override protected void onStop() {
        releasePlayer();
        if (nowPlaying != null) nowPlaying.setText("Playback stopped");
        if (playPause != null) playPause.setText("Play");
        if (seekBar != null) seekBar.setProgress(0);
        if (timeLabel != null) timeLabel.setText("0:00 / 0:00");
        if (seekBar != null) seekBar.setEnabled(false);
        renderItems();
        super.onStop();
    }

    private void loadItems() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                items.add(new AudioItem(o.optString("book", "Other"), o.optString("name", "Audio"), o.getString("uri")));
            }
        } catch (Exception ignored) { items.clear(); }
    }

    private void saveItems() {
        JSONArray array = new JSONArray();
        for (AudioItem item : items) {
            JSONObject o = new JSONObject();
            try {
                o.put("book", item.book);
                o.put("name", item.name);
                o.put("uri", item.uri);
                array.put(o);
            } catch (Exception ignored) { }
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private static final class AudioItem {
        String book, name;
        final String uri;
        AudioItem(String book, String name, String uri) { this.book = book; this.name = name; this.uri = uri; }
    }
}
