package com.example.app.media.download;

import android.content.Context;
import androidx.annotation.NonNull;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.dao.DownloadedContentDao;
import com.example.app.data.local.dao.SermonDao;
import com.example.app.data.local.entity.DownloadedContentEntity;
import com.example.app.data.local.entity.SermonEntity;
import com.example.app.data.mapper.DownloadedContentMapper;
import com.example.app.domain.model.DownloadedContent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Local Download Manager responsible for managing device-local media assets.
 * Maintains strict separation between physical device downloads (DownloadedContent)
 * and sermon/content domain metadata (Sermon) per Master Directive §21.
 */
public class DownloadManager {

    public static final String STATUS_QUEUED = DownloadStatus.QUEUED.name();
    public static final String STATUS_DOWNLOADING = DownloadStatus.DOWNLOADING.name();
    public static final String STATUS_COMPLETED = DownloadStatus.COMPLETED.name();
    public static final String STATUS_FAILED = DownloadStatus.FAILED.name();

    private final Context context;
    private final File downloadsDir;
    private final DownloadedContentDao downloadedContentDao;
    private final SermonDao sermonDao;

    public DownloadManager(@NonNull Context context) {
        this(context,
             PerazimDatabase.getInstance(context).downloadedContentDao(),
             PerazimDatabase.getInstance(context).sermonDao());
    }

    public DownloadManager(Context context, DownloadedContentDao downloadedContentDao) {
        this(context, downloadedContentDao,
             context != null ? PerazimDatabase.getInstance(context).sermonDao() : null);
    }

    public DownloadManager(Context context, DownloadedContentDao downloadedContentDao, SermonDao sermonDao) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.downloadedContentDao = downloadedContentDao;
        this.sermonDao = sermonDao;

        File baseDir = (this.context != null && this.context.getFilesDir() != null)
                ? this.context.getFilesDir()
                : new File(System.getProperty("java.io.tmpdir", "/tmp"));
        this.downloadsDir = new File(baseDir, "downloads");
        if (!this.downloadsDir.exists()) {
            this.downloadsDir.mkdirs();
        }
    }

    public File getDownloadsDirectory() {
        return downloadsDir;
    }

    /**
     * Enqueues a download task and registers it in DownloadedContentDao with status QUEUED.
     */
    public DownloadedContentEntity enqueueDownload(String contentType, String contentId) {
        if (contentId == null) {
            return null;
        }
        DownloadedContentEntity existing = findEntity(contentId);
        String id = (existing != null) ? existing.getId() : "dl_" + contentId;

        DownloadedContentEntity entity = new DownloadedContentEntity(
                id,
                contentType != null ? contentType : "SERMON",
                contentId,
                existing != null && existing.getLocalUri() != null ? existing.getLocalUri() : "",
                0L,
                System.currentTimeMillis(),
                STATUS_QUEUED
        );

        if (downloadedContentDao != null) {
            downloadedContentDao.insert(entity);
        }
        return entity;
    }

    public DownloadedContentEntity enqueue(String contentType, String contentId) {
        return enqueueDownload(contentType, contentId);
    }

    /**
     * Updates download status to DOWNLOADING.
     */
    public void markDownloading(String contentId) {
        if (contentId == null || downloadedContentDao == null) {
            return;
        }
        DownloadedContentEntity entity = findEntity(contentId);
        if (entity != null) {
            entity.setStatus(STATUS_DOWNLOADING);
            downloadedContentDao.update(entity);
        }
    }

    /**
     * Saves a simulated or downloaded media payload to the local downloads directory.
     */
    public File saveSimulatedPayload(String fileName, byte[] payload) throws IOException {
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
        File targetFile = new File(downloadsDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            if (payload != null) {
                fos.write(payload);
            }
            fos.flush();
        }
        return targetFile;
    }

    public File saveSimulatedPayload(String contentId, String extension, byte[] payload) throws IOException {
        String ext = (extension != null && extension.startsWith(".")) ? extension : "." + extension;
        return saveSimulatedPayload(contentId + ext, payload);
    }

    /**
     * Completes a download task, records physical file path and size in DownloadedContentDao,
     * and marks the corresponding Sermon as downloaded in SermonDao if applicable.
     */
    public DownloadedContentEntity completeDownload(String contentType, String contentId, File localFile) {
        String localPath = (localFile != null) ? localFile.getAbsolutePath() : "";
        long fileSize = (localFile != null && localFile.exists()) ? localFile.length() : 0L;
        return completeDownload(contentType, contentId, localPath, fileSize);
    }

    public DownloadedContentEntity completeDownload(String contentId, File localFile) {
        DownloadedContentEntity existing = findEntity(contentId);
        String contentType = (existing != null && existing.getContentType() != null)
                ? existing.getContentType()
                : "SERMON";
        return completeDownload(contentType, contentId, localFile);
    }

    public DownloadedContentEntity completeDownload(String contentType, String contentId, String localPath, long fileSizeBytes) {
        if (contentId == null) {
            return null;
        }
        DownloadedContentEntity existing = findEntity(contentId);
        String id = (existing != null) ? existing.getId() : "dl_" + contentId;

        DownloadedContentEntity completedEntity = new DownloadedContentEntity(
                id,
                contentType != null ? contentType : "SERMON",
                contentId,
                localPath != null ? localPath : "",
                fileSizeBytes,
                System.currentTimeMillis(),
                STATUS_COMPLETED
        );

        if (downloadedContentDao != null) {
            downloadedContentDao.insert(completedEntity);
        }

        // Maintain separation: update SermonDao metadata pointer if content is a sermon
        if (sermonDao != null && ("SERMON".equalsIgnoreCase(contentType) || sermonDao.getSermonById(contentId) != null)) {
            markAsDownloaded(sermonDao, contentId, localPath);
        }

        return completedEntity;
    }

    /**
     * Static helper to mark a sermon as downloaded in SermonDao.
     */
    public static void markAsDownloaded(SermonDao sermonDao, String sermonId, String localPath) {
        if (sermonDao == null || sermonId == null) {
            return;
        }
        SermonEntity sermon = sermonDao.getSermonById(sermonId);
        if (sermon != null) {
            sermon.setDownloaded(true);
            sermon.setLocalAudioPath(localPath != null ? localPath : "");
            sermonDao.update(sermon);
        }
    }

    public void markAsDownloaded(String sermonId, String localPath) {
        markAsDownloaded(this.sermonDao, sermonId, localPath);
    }

    /**
     * Marks a download task as FAILED.
     */
    public void markFailed(String contentId, String error) {
        if (contentId == null || downloadedContentDao == null) {
            return;
        }
        DownloadedContentEntity entity = findEntity(contentId);
        if (entity != null) {
            entity.setStatus(STATUS_FAILED);
            downloadedContentDao.update(entity);
        }
    }

    /**
     * Deletes physical media payload file and purges the record from DownloadedContentDao.
     * Also clears local path from SermonDao.
     */
    public boolean deleteDownload(String contentId) {
        if (contentId == null) {
            return false;
        }
        DownloadedContentEntity entity = findEntity(contentId);
        if (entity != null) {
            if (entity.getLocalUri() != null && !entity.getLocalUri().isEmpty()) {
                File file = new File(entity.getLocalUri());
                if (file.exists()) {
                    file.delete();
                }
            }
            if (downloadedContentDao != null) {
                downloadedContentDao.delete(entity);
                downloadedContentDao.deleteById(entity.getId());
            }
        }

        // Ensure conventional file is also removed if present
        File fallbackFile = new File(downloadsDir, contentId + ".mp3");
        if (fallbackFile.exists()) {
            fallbackFile.delete();
        }

        // Reset SermonEntity if applicable
        if (sermonDao != null) {
            SermonEntity sermon = sermonDao.getSermonById(contentId);
            if (sermon != null) {
                sermon.setDownloaded(false);
                sermon.setLocalAudioPath("");
                sermonDao.update(sermon);
            }
        }
        return true;
    }

    /**
     * Checks if the content is downloaded and completed.
     */
    public boolean isDownloaded(String contentId) {
        DownloadedContentEntity entity = findEntity(contentId);
        if (entity == null) {
            return false;
        }
        return STATUS_COMPLETED.equalsIgnoreCase(entity.getStatus());
    }

    public DownloadStatus getDownloadStatus(String contentId) {
        DownloadedContentEntity entity = findEntity(contentId);
        if (entity == null || entity.getStatus() == null) {
            return null;
        }
        try {
            return DownloadStatus.valueOf(entity.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public DownloadedContent getDownloadedContent(String contentId) {
        DownloadedContentEntity entity = findEntity(contentId);
        return DownloadedContentMapper.toDomain(entity);
    }

    public List<DownloadedContent> getAllDownloads() {
        if (downloadedContentDao == null) {
            return Collections.emptyList();
        }
        return DownloadedContentMapper.toDomainList(downloadedContentDao.getAllDownloaded());
    }

    /**
     * Locates a DownloadedContentEntity by direct id, prefixed id, type+id, or contentId scan.
     */
    public DownloadedContentEntity findEntity(String contentId) {
        if (downloadedContentDao == null || contentId == null) {
            return null;
        }
        DownloadedContentEntity entity = downloadedContentDao.getContentById(contentId);
        if (entity != null) {
            return entity;
        }
        entity = downloadedContentDao.getContentById("dl_" + contentId);
        if (entity != null) {
            return entity;
        }
        entity = downloadedContentDao.getContentByTypeAndId("SERMON", contentId);
        if (entity != null) {
            return entity;
        }
        List<DownloadedContentEntity> all = downloadedContentDao.getAllDownloaded();
        if (all != null) {
            for (DownloadedContentEntity e : all) {
                if (contentId.equals(e.getContentId()) || contentId.equals(e.getId())) {
                    return e;
                }
            }
        }
        return null;
    }
}
