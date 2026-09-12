package com.example.app.data.repository;

import com.example.app.data.local.dao.HymnDao;
import com.example.app.data.local.entity.HymnEntity;
import com.example.app.data.mapper.HymnMapper;
import com.example.app.domain.model.Hymn;
import com.example.app.domain.repository.HymnRepository;

import java.util.List;

/**
 * Room implementation of HymnRepository.
 */
public class RoomHymnRepository implements HymnRepository {

    private final HymnDao hymnDao;

    public RoomHymnRepository(HymnDao hymnDao) {
        this.hymnDao = hymnDao;
    }

    @Override
    public List<Hymn> getAllHymns() {
        return HymnMapper.toDomainList(hymnDao.getAllHymns());
    }

    @Override
    public Hymn getHymnByNumber(int number) {
        return HymnMapper.toDomain(hymnDao.getHymnByNumber(number));
    }

    @Override
    public Hymn getHymnById(String id) {
        if (id == null) {
            return null;
        }
        return HymnMapper.toDomain(hymnDao.getHymnById(id));
    }

    @Override
    public List<Hymn> searchHymns(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllHymns();
        }
        return HymnMapper.toDomainList(hymnDao.searchHymns(query.trim()));
    }

    @Override
    public List<Hymn> getFavoriteHymns() {
        return HymnMapper.toDomainList(hymnDao.getFavoriteHymns());
    }

    @Override
    public void toggleFavorite(String hymnId, boolean isFavorite) {
        if (hymnId == null) {
            return;
        }
        HymnEntity entity = hymnDao.getHymnById(hymnId);
        if (entity != null) {
            entity.setFavorite(isFavorite);
            hymnDao.update(entity);
        }
    }
}
