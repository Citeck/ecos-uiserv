package ru.citeck.ecos.uiserv.domain.userconfig.repo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConfigurationsRepository extends JpaRepository<UserConfigurationEntity, Long> {
    UserConfigurationEntity findByExternalId(String externalId);

    int countByUserName(String userName);

    UserConfigurationEntity findTopByUserNameOrderByCreationTimeAsc(String userName);

    Integer deleteByExternalId(String externalId);
}
