package ru.citeck.ecos.uiserv.service.evaluator;

import lombok.NonNull;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.service.Evaluated;

/**
 * @author Roman Makarskiy
 */
@Service
public class RecordEvaluatorService implements ApplicationContextAware {

    private ApplicationContext context;

    public boolean evaluate(@NonNull Evaluated evaluated, @NonNull RecordRef recordRef) {
        String id = evaluated.getId();
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Evaluated id cannot be empty");
        }

        RecordEvaluator bean = context.getBean(id, RecordEvaluator.class);

        return bean.evaluate(evaluated.getConfig(), recordRef);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
