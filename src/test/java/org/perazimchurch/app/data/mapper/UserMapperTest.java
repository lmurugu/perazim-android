package org.perazimchurch.app.data.mapper;

import org.junit.Test;
import org.perazimchurch.app.data.local.entity.UserEntity;

import static org.junit.Assert.assertEquals;

public class UserMapperTest {
    @Test
    public void preservesStoredZeroGracePointsWithoutContext() {
        UserEntity member = member("mapper-zero", 0);
        assertEquals(0, UserMapper.toDomain(member).getGracePoints());
    }

    @Test
    public void preservesExistingPositiveGraceBalance() {
        UserEntity member = member("mapper-existing", 7);
        assertEquals(7, UserMapper.toDomain(member).getGracePoints());
    }

    private UserEntity member(String id, int grace) {
        return new UserEntity(id, "Member", "member@example.test", "", "MEMBER",
                "", "", 0L, 0L, true, 0, 0, grace, false);
    }
}
