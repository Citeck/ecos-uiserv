package ru.citeck.ecos.uiserv.service.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.utils.StringUtils;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NormalFormProvider implements FormProvider, MutableFormProvider {

    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Override
    public EcosFormModel getFormByKey(String formKey) {
        List<File> found = fileService.find("formKey", Collections.singletonList(formKey));
        if (found.isEmpty()) {
            return null;
        }
        if (found.size() > 1) {
            String forms = found.stream()
                                .map(this::getFormId)
                                .collect(Collectors.joining("', '"));
            log.warn("More than one form found by key: {} forms: '{}'",
                formKey, forms);
        }
        return fromJson(found.get(0));
    }

    @Override
    public EcosFormModel getFormByKeyAndMode(String formKey, String formMode) {
        Map<String,String> metaFields = new HashMap<>();
        metaFields.put("formKey", formKey);
        metaFields.put("formMode", StringUtils.isBlank(formMode) ? null : formMode);

        List<File> found = fileService.findByMeta(metaFields);
        if (found.isEmpty()) {
            return null;
        }
        if (found.size() > 1) {
            String forms = found.stream()
                .map(this::getFormId)
                .collect(Collectors.joining("', '"));
            log.warn("More than one form found by (key, mode): ({}, {}) forms: '{}'",
                formKey, formMode, forms);
        }
        return fromJson(found.get(0));
    }

    private String getFormId(File file) {
        try {
            EcosFormModel model = fromJson(file);
            return model.getId();
        } catch (Exception e) {
            log.warn("Form parsing error", e);
            return "IncorrectFile[id='" + file.getId() + "':'" + file.getFileId() + "']";
        }
    }

    @Override
    public EcosFormModel getFormById(String id) {
        return fileService.loadFile(FileType.FORM, id)
            .map(this::fromJson)
            .orElse(null);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private EcosFormModel fromJson(File file) {
        try {
            return objectMapper.readValue(file.getFileVersion().getBytes(), EcosFormModel.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] toJson(EcosFormModel form) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, form);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(EcosFormModel model) {
        Map<String, String> modelMeta = new HashMap<>();
        modelMeta.put("formKey", model.getFormKey());
        modelMeta.put("formMode", model.getFormMode());

        fileService.deployFileOverride(FileType.FORM, model.getId(), null, toJson(model),
            modelMeta);
    }

    @Override
    public void create(EcosFormModel model) {
        save(model);
    }

    @Override
    public void delete(String formId) {
        fileService.delete(FileType.FORM, formId);
    }
}
