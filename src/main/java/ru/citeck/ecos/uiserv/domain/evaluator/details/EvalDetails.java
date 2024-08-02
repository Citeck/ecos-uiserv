package ru.citeck.ecos.uiserv.domain.evaluator.details;

import java.util.List;

public interface EvalDetails {

    boolean getResult();

    List<EvalResultCause> getCauses();
}
