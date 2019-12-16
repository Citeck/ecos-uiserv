package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.ActionEntity;

public interface ActionRepository extends JpaRepository<ActionEntity, Long> {

    ActionEntity findByExtId(String extId);
}
