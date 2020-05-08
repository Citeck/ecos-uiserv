package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.I18nEntity;

import java.util.Optional;

public interface I18nRepository extends JpaRepository<I18nEntity, Long> {

    Optional<I18nEntity> findByTenantAndExtId(String tenant, String extId);
}
