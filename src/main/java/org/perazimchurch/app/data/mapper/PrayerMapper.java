package org.perazimchurch.app.data.mapper;

import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.PrayerEntity;
import org.perazimchurch.app.domain.model.Prayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between Room {@link PrayerEntity} and domain {@link Prayer}.
 */
public final class PrayerMapper {

    private PrayerMapper() {
        // Utility class
    }

    /**
     * Maps a {@link PrayerEntity} to a domain {@link Prayer}.
     */
    @Nullable
    public static Prayer toDomain(@Nullable PrayerEntity entity) {
        return toDomain(entity, false);
    }

    /**
     * Maps a {@link PrayerEntity} to a domain {@link Prayer} with user amen status.
     */
    @Nullable
    public static Prayer toDomain(@Nullable PrayerEntity entity, boolean userHasAmened) {
        if (entity == null) {
            return null;
        }

        Prayer prayer = new Prayer();
        prayer.setId(entity.getId());
        prayer.setTitle(entity.getTitle());
        prayer.setBody(entity.getContent());
        prayer.setAuthorId(entity.getUserId());
        prayer.setAuthorName(entity.isAnonymous() ? "Anonymous" : entity.getAuthorName());
        prayer.setVisibility(entity.isAnonymous() ? Prayer.Visibility.PRIVATE : Prayer.Visibility.PUBLIC);
        prayer.setAmenCount(entity.getPrayerCount());
        prayer.setCreatedAt(entity.getCreatedMillis());
        prayer.setAnswered(entity.isAnswered());
        prayer.setUserHasAmened(userHasAmened);

        return prayer;
    }

    /**
     * Maps a domain {@link Prayer} to a Room {@link PrayerEntity}.
     */
    @Nullable
    public static PrayerEntity toEntity(@Nullable Prayer domain) {
        if (domain == null) {
            return null;
        }

        String id = domain.getId() != null ? domain.getId() : UUID.randomUUID().toString();
        long createdMillis = domain.getCreatedAt() > 0 ? domain.getCreatedAt() : System.currentTimeMillis();
        boolean isAnonymous = "Anonymous".equalsIgnoreCase(domain.getAuthorName())
                || domain.getVisibility() == Prayer.Visibility.PRIVATE;

        return new PrayerEntity(
                id,
                domain.getAuthorId(),
                domain.getAuthorName(),
                domain.getTitle(),
                domain.getBody(),
                isAnonymous,
                domain.isAnswered(),
                domain.getAmenCount(),
                createdMillis
        );
    }

    /**
     * Maps a list of {@link PrayerEntity} to a list of domain {@link Prayer}.
     */
    public static List<Prayer> toDomainList(@Nullable List<PrayerEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Prayer> list = new ArrayList<>(entities.size());
        for (PrayerEntity entity : entities) {
            Prayer prayer = toDomain(entity);
            if (prayer != null) {
                list.add(prayer);
            }
        }
        return list;
    }

    /**
     * Maps a list of domain {@link Prayer} to a list of {@link PrayerEntity}.
     */
    public static List<PrayerEntity> toEntityList(@Nullable List<Prayer> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<PrayerEntity> list = new ArrayList<>(domains.size());
        for (Prayer domain : domains) {
            PrayerEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
