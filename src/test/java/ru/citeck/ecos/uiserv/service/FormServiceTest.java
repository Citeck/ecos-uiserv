package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class FormServiceTest {

    @Autowired
    private EcosFormService formService;

    @Test
    public void test() {

        String id = "test";

        EcosFormModel model = new EcosFormModel();
        model.setId(id);
        model.setTitle("test");
        model.setDefinition(JsonNodeFactory.instance.objectNode());

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
        modelA.setDescription("Test form");
        modelA.setTitle("Test Form");
        modelA.setDefinition(new JsonNodeFactory(false).objectNode());
        modelA.setFormKey("A");

        formService.save(modelA);

        assertThat(formService.getFormById("TEST_FORM_A"), is(Optional.of(modelA)));

        modelA.setFormKey("B");
        modelA.setFormMode("X");
        formService.save(modelA);

        Optional<EcosFormModel> modelA_ = formService.getFormById("TEST_FORM_A");
        assertThat(modelA_.get().getFormKey(), is("B"));
        assertThat(modelA_.get().getFormMode(), is("X"));

        modelA.setFormKey("C");
        modelA.setFormMode(null);
        formService.save(modelA);

        modelA_ = formService.getFormById("TEST_FORM_A");
        assertThat(modelA_.get().getFormKey(), is("C"));
        assertThat(modelA_.get().getFormMode(), nullValue());
    }

    @Test
    public void testFormModeSearch() {
        EcosFormModel modelA = new EcosFormModel();
        modelA.setId("TEST_FORM_A");
        modelA.setDescription("Test form");
        modelA.setTitle("Test Form");
        modelA.setDefinition(new JsonNodeFactory(false).objectNode());
        modelA.setFormKey("K");

        EcosFormModel modelB = new EcosFormModel(modelA);
        modelB.setId("TEST_FORM_B");
        modelB.setFormKey("K");
        modelB.setFormMode("X");

        EcosFormModel modelC = new EcosFormModel(modelA);
        modelC.setId("TEST_FORM_C");
        modelC.setFormKey("K");
        modelC.setFormMode("Y");

        EcosFormModel modelD = new EcosFormModel(modelA);
        modelD.setId("TEST_FORM_D");
        modelD.setFormKey("L");
        modelD.setFormMode("X");

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
        assertThat(
            formService.getFormByKeyAndMode("K", "X"),
            is(Optional.of(modelB)));
        assertThat(
            formService.getFormByKeyAndMode("K", "Y"),
            is(Optional.of(modelC)));
        assertThat(
            formService.getFormByKeyAndMode("K", "Z"),
            is(Optional.empty()));
        assertThat(
            formService.getFormByKeyAndMode("K", null),
            is(Optional.of(modelA)));
        assertThat(
            formService.getFormByKeyAndMode("K", ""),
            is(Optional.of(modelA)));

        formService.delete(modelA.getId());
        assertThat(
            formService.getFormByKey("K"),
            is(Optional.of(modelB)));
    }
}
