package ru.citeck.ecos.uiserv.domain.action.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActionRepository extends JpaRepository<ActionEntity, Long>,
    JpaSpecificationExecutor<ActionEntity> {

    ActionEntity findByExtId(String extId);
}
