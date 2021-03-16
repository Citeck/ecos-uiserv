package ru.citeck.ecos.uiserv.domain.form;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class FormServiceTest {

    @Autowired
    private EcosFormService formService;

    @Test
    public void test() {

        String id = "test";

        EcosFormModel model = new EcosFormModel();
        model.setId(id);
        model.setTitle(new MLText("test"));
        model.setDefinition(ObjectData.create());

        formService.save(model);

        assertThat(formService.getFormById(id), is(Optional.of(model)));

        formService.delete(id);

        assertThat(formService.getFormById(id), is(Optional.empty()));

        formService.save(model);

        assertThat(formService.getFormById(id), is(Optional.of(model)));
    }

    @Test
    public void testMetaOverwrite() {
        EcosFormModel modelA = new EcosFormModel();
        modelA.setId("TEST_FORM_A");
        modelA.setDescription(new MLText("Test form"));
        modelA.setTitle(new MLText("Test Form"));
        modelA.setDefinition(ObjectData.create());
        modelA.setFormKey("A");

        formService.save(modelA);

        assertThat(formService.getFormById("TEST_FORM_A"), is(Optional.of(modelA)));

        modelA.setFormKey("B");
        formService.save(modelA);

        Optional<EcosFormModel> modelA_ = formService.getFormById("TEST_FORM_A");
        assertThat(modelA_.get().getFormKey(), is("B"));

        modelA.setFormKey("C");
        formService.save(modelA);

        modelA_ = formService.getFormById("TEST_FORM_A");
        assertThat(modelA_.get().getFormKey(), is("C"));
    }

    @Test
    public void testFormModeSearch() {
        EcosFormModel modelA = new EcosFormModel();
        modelA.setId("TEST_FORM_A");
        modelA.setDescription(new MLText("Test form"));
        modelA.setTitle(new MLText("Test Form"));
        modelA.setDefinition(ObjectData.create());
        modelA.setFormKey("K");

        EcosFormModel modelB = new EcosFormModel(modelA);
        modelB.setId("TEST_FORM_B");
        modelB.setFormKey("K");

        EcosFormModel modelC = new EcosFormModel(modelA);
        modelC.setId("TEST_FORM_C");
        modelC.setFormKey("K");

        EcosFormModel modelD = new EcosFormModel(modelA);
        modelD.setId("TEST_FORM_D");
        modelD.setFormKey("L");

        formService.save(modelA);
        formService.save(modelB);
        formService.save(modelC);
        formService.save(modelD);

        assertThat(
            formService.getFormByKey("NOT_EXIST"),
            is(Optional.empty()));
        assertThat(
            formService.getFormsByKeys(Arrays.asList("NOT_EXISTS", "NOT_EXISTS_TOO")),
            is(Collections.emptyList()));
        assertThat(
            formService.getFormByKey("K"),
            is(Optional.of(modelA)));

        formService.delete(modelA.getId());
        assertThat(
            formService.getFormByKey("K"),
            is(Optional.of(modelB)));
    }
}
