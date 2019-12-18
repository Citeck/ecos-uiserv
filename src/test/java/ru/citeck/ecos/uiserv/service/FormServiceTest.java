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

import java.util.Optional;

import static org.hamcrest.Matchers.*;
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
}
