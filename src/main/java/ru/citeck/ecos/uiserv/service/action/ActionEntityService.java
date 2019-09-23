package ru.citeck.ecos.uiserv.service.action;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.action.Action;
import ru.citeck.ecos.uiserv.domain.action.ActionDtoFactory;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.repository.ActionRepository;
import ru.citeck.ecos.uiserv.service.entity.AbstractBaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Roman Makarskiy
 */
@Service
public class ActionEntityService extends AbstractBaseEntityService<ActionDTO> {

    private final ActionRepository actionRepository;

    @Autowired
    public ActionEntityService(ActionRepository actionRepository) {
        super(ActionDTO.class, null);
        this.actionRepository = actionRepository;
    }

    @Override
    public ActionDTO create(ActionDTO entity) {
        if (StringUtils.isBlank(entity.getId())) {
            entity.setId(UUID.randomUUID().toString());
        }
        return save(entity);
    }

    @Override
    public ActionDTO update(ActionDTO entity) {
        if (StringUtils.isBlank(entity.getId())) {
            throw new IllegalArgumentException("Cannot update entity with blank id");
        }
        return save(entity);
    }

    private ActionDTO save(ActionDTO entity) {
        Action action = ActionDtoFactory.fromDto(entity);
        Action savedAction = actionRepository.save(action);
        return ActionDtoFactory.fromAction(savedAction);
    }

    @Override
    public Optional<ActionDTO> getById(String id) {
        Optional<Action> actionOpt = actionRepository.findById(id);
        if (!actionOpt.isPresent()) {
            return Optional.empty();
        }

        ActionDTO actionDTO = ActionDtoFactory.fromAction(actionOpt.get());

        return Optional.of(actionDTO);
    }

    @Override
    public void delete(String id) {
        actionRepository.deleteById(id);
    }

    @Override
    public Optional<ActionDTO> getByRecord(RecordRef recordRef) {
        return Optional.empty();
    }

    @Override
    public Optional<ActionDTO> getByKey(String type, String key) {
        throw new RuntimeException("Unsupported operation");
    }

    @Override
    public Optional<ActionDTO> getByKeys(String type, List<String> keys) {
        throw new RuntimeException("Unsupported operation");
    }

}
