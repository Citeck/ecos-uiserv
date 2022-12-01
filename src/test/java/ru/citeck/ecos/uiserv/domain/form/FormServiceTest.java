package ru.citeck.ecos.uiserv.domain.form;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class FormServiceTest {

    @Autowired
    private EcosFormService formService;

    @Test
    public void test() {

        String id = "test";

        EcosFormDef model = EcosFormDef.create()
            .withId(id)
            .withTitle(new MLText("test"))
            .withDefinition(ObjectData.create())
            .build();

        formService.save(model);

        assertThat(formService.getFormById(id), is(Optional.of(model)));

        formService.delete(id);

        assertThat(formService.getFormById(id), is(Optional.empty()));

        formService.save(model);

        assertThat(formService.getFormById(id), is(Optional.of(model)));
    }

    @Test
    public void testMetaOverwrite() {

        EcosFormDef modelA = EcosFormDef.create()
            .withId("TEST_FORM_A")
            .withDescription(new MLText("Test form"))
            .withTitle(new MLText("Test Form"))
            .withDefinition(ObjectData.create())
            .withFormKey("A")
            .build();

        formService.save(modelA);

        assertThat(formService.getFormById("TEST_FORM_A"), is(Optional.of(modelA)));

        modelA = modelA.copy().withFormKey("B").build();
        formService.save(modelA);

        Optional<EcosFormDef> modelA_ = formService.getFormById("TEST_FORM_A");
        assertThat(modelA_.get().getFormKey(), is("B"));

        modelA = modelA.copy().withFormKey("C").build();
        formService.save(modelA);

        modelA_ = formService.getFormById("TEST_FORM_A");
        assertThat(modelA_.get().getFormKey(), is("C"));
    }

    @Test
    public void testFormModeSearch() {

        EcosFormDef modelA = EcosFormDef.create()
            .withId("TEST_FORM_A")
            .withDescription(new MLText("Test form"))
            .withTitle(new MLText("Test Form"))
            .withDefinition(ObjectData.create())
            .withFormKey("K")
            .build();

        EcosFormDef modelB = new EcosFormDef.Builder(modelA)
            .withId("TEST_FORM_B")
            .withFormKey("K")
            .build();

        EcosFormDef modelC = new EcosFormDef.Builder(modelA)
            .withId("TEST_FORM_C")
            .withFormKey("K")
            .build();

        EcosFormDef modelD = new EcosFormDef.Builder(modelA)
            .withId("TEST_FORM_D")
            .withFormKey("L")
            .build();

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
