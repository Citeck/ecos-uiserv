package ru.citeck.ecos.uiserv.domain.i18n.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface I18nRepository extends JpaRepository<I18nEntity, Long> {

    Optional<I18nEntity> findByTenantAndExtId(String tenant, String extId);
}
