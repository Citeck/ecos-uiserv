package ru.citeck.ecos.uiserv.service.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.EcosFormModel;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
            throw new RuntimeException("More than one form found by key: " + formKey);
        }
        return fromJson(found.iterator().next());
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
        fileService.deployFileOverride(FileType.FORM, model.getId(), null, toJson(model),
            Collections.singletonMap("formKey", model.getFormKey()));
    }

    @Override
    public void create(EcosFormModel model) {
        save(model);
    }

    @Override
    public void delete(String formId) {
        fileService.deployFileOverride(FileType.FORM, formId, null, null, null)
            .getFileId();
    }
}
