package ru.citeck.ecos.uiserv.domain.form.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NormalFormProvider implements FormProvider, MutableFormProvider {

    //todo: Rework WITHOUT FileService and with own table "ecos-forms"
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Override
    public int getCount() {
        return fileService.getCount(FileType.FORM);
    }

    public List<EcosFormModel> getAllForms(int max, int skip) {
        return fileService.findByType(FileType.FORM, max, skip)
            .stream()
            .map(f -> {
                try {
                    return Optional.ofNullable(fromJson(f));
                } catch (Exception e) {
                    return Optional.<EcosFormModel>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public EcosFormModel getFormByKey(String formKey) {
        List<File> found = fileService.find("formKey", Collections.singletonList(formKey));
        if (found.isEmpty()) {
            return null;
        }
        if (found.size() > 1) {
            String forms = found
                .stream()
                .map(this::getFormId)
                .collect(Collectors.joining("', '"));
            log.warn("More than one form found by key: {} forms: '{}'",
                formKey, forms);

            File foundFile = found
                .stream()
                .sorted((o1, o2) ->
                    Comparator.nullsFirst(String::compareTo).compare(
                        o1.getFileMeta().get("formMode"),
                        o2.getFileMeta().get("formMode")))
                .collect(Collectors.toList())
                .get(0);
            return fromJson(foundFile);
        }
        return fromJson(found.get(0));
    }

    @Override
    public EcosFormModel getFormByKeyAndMode(String formKey, String formMode) {
        List<File> found = fileService.find("formKey", Collections.singletonList(formKey));

        Predicate<File> formModeFilter = StringUtils.isBlank(formMode) ?
            file -> StringUtils.isBlank(file.getFileMeta().get("formMode")) :
            file -> formMode.equals(file.getFileMeta().get("formMode"));

        found.removeIf(formModeFilter.negate());

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
