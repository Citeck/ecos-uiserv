package ru.citeck.ecos.uiserv.domain.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Roman Makarskiy
 */
abstract class ParamsActionMixIn {

    @JsonProperty("params")
    abstract JsonNode getConfig();

}
