package ru.citeck.ecos.uiserv.domain.admin.api.records.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface AdminSecGroupRepository extends JpaRepository<AdminSecGroupEntity, Long> {
}
