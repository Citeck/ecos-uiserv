package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.UserConfigurationEntity;

public interface UserConfigurationsRepository extends JpaRepository<UserConfigurationEntity, Long> {
    UserConfigurationEntity findByExternalId(String externalId);

    int countByUserName(String userName);

    UserConfigurationEntity findTopByUserNameOrderByCreationTimeAsc(String userName);

    Integer deleteByExternalId(String externalId);
}
