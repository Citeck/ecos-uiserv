package ru.citeck.ecos.uiserv.domain.action.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity;

public interface ActionRepository extends JpaRepository<ActionEntity, Long> {

    ActionEntity findByExtId(String extId);
}
