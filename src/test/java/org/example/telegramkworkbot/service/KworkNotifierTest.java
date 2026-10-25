package org.example.telegramkworkbot.service;

import org.example.telegramkworkbot.entities.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KworkNotifierTest {

    @Test
    void groupsUsersBySubcategory() {
        Map<Long, List<Long>> result = KworkNotifier.subscribersByCategory(List.of(
                user(1L, 44L, 43L),
                user(2L, 44L),
                user(3L)));

        assertThat(result).containsOnlyKeys(44L, 43L);
        assertThat(result.get(44L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.get(43L)).containsExactly(1L);
    }

    private static UserEntity user(Long id, Long... subs) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSubcategoryIds(new java.util.HashSet<>(Set.of(subs)));
        return user;
    }
}
