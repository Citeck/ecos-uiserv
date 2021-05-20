package ru.citeck.ecos.uiserv.domain.admin.api.records.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSecGroupRepository extends JpaRepository<AdminSecGroupEntity, Long> {

    Optional<AdminSecGroupEntity> findByExternalId(String extId);
}
