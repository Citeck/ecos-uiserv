package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.repository.CrudRepository;
import ru.citeck.ecos.uiserv.domain.action.Action;

public interface ActionRepository extends CrudRepository<Action, String> {
}
