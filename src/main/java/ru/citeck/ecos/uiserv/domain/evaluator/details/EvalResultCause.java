package ru.citeck.ecos.uiserv.domain.evaluator.details;

import ru.citeck.ecos.commons.data.ObjectData;

public interface EvalResultCause {

    String getMessage();

    String getLocalizedMessage();

    String getType();

    ObjectData getData();
}
