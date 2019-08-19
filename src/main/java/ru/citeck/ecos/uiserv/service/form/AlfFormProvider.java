package ru.citeck.ecos.uiserv.service.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;

import java.util.Collections;

@Component
public class AlfFormProvider implements FormProvider, MutableFormProvider {

    private RecordsService recordsService;
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public AlfFormProvider(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @Override
    public EcosFormModel getFormByKey(String formKey) {

        EcosFormRecords.Query formsQuery = new EcosFormRecords.Query();
        formsQuery.setFormKey(formKey);

        RecordsQuery query = new RecordsQuery();
        query.setQuery(formsQuery);
        query.setSourceId("alfresco@eform");

        return recordsService.queryRecord(query, EcosFormModel.class).orElse(null);
    }

    @Override
    public EcosFormModel getFormById(String id) {
        RecordRef ref = RecordRef.valueOf("alfresco@eform@" + id);
        return recordsService.getMeta(ref, EcosFormModel.class);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void save(EcosFormModel model) {

        RecordMeta meta = new RecordMeta("alfresco@eform@" + model.getId());
        ObjectNode attributes = mapper.valueToTree(model);
        attributes.remove("id");
        meta.setAttributes(attributes);

        RecordsMutation mutation = new RecordsMutation();
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);
    }

    @Override
    public void create(EcosFormModel model) {
        save(model);
    }

    @Override
    public void delete(String formId) {

        RecordRef ref = RecordRef.valueOf("alfresco@eform@" + formId);
        RecordsDeletion deletion = new RecordsDeletion();
        deletion.setRecords(Collections.singletonList(ref));

        recordsService.delete(deletion);
    }
}
