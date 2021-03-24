package ru.citeck.ecos.uiserv.domain.action.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ActionDto {

    private String id;

    private MLText name;
    private MLText pluralName;

    private String icon;
    private RecordRef typeRef;

    private String preActionModule;

    private ActionConfirmDef confirm;

    private ActionResultDto result;

    private String type;
    private ObjectData config = ObjectData.create();

    private Predicate predicate;
    private RecordEvaluatorDto evaluator;

    private Map<String, Boolean> features = new HashMap<>();

    public ActionDto() {
    }

    public ActionDto(ActionDto other) {

        this.id = other.id;
        this.name = other.name;
        this.pluralName = other.pluralName;
        this.type = other.type;
        this.icon = other.icon;
        this.predicate = Json.getMapper().copy(other.predicate);
        this.confirm = Json.getMapper().copy(other.confirm);
        this.result = Json.getMapper().copy(other.result);
        this.evaluator = other.evaluator;
        this.typeRef = other.typeRef;
        this.preActionModule = other.preActionModule;

        this.config = ObjectData.deepCopy(other.config);
        this.features = DataValue.create(features).asMap(String.class, Boolean.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MLText getName() {
        return name;
    }

    public void setName(MLText name) {
        this.name = name;
    }

    public MLText getPluralName() {
        return pluralName;
    }

    public void setPluralName(MLText pluralName) {
        this.pluralName = pluralName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public RecordRef getTypeRef() {
        return typeRef;
    }

    public void setTypeRef(RecordRef typeRef) {
        this.typeRef = typeRef;
    }

    public ActionConfirmDef getConfirm() {
        return confirm;
    }

    public void setConfirm(ActionConfirmDef confirm) {
        this.confirm = confirm;
    }

    public ActionResultDto getResult() {
        return result;
    }

    public void setResult(ActionResultDto result) {
        this.result = result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectData getConfig() {
        return config;
    }

    public void setConfig(ObjectData config) {
        this.config = config;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public RecordEvaluatorDto getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(RecordEvaluatorDto evaluator) {
        this.evaluator = evaluator;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Boolean> features) {
        this.features = features;
    }
}
