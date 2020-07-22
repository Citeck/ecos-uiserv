package ru.citeck.ecos.uiserv.app.web;

import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestEntityRecordUtil {

    public static final String URL_RECORDS_MUTATE = "/api/records/mutate";
    public static final String URL_RECORDS_QUERY = "/api/records/query";
    public static final String URL_RECORDS_DELETE = "/api/records/delete";

    public static void performMutateAndCheckResponseId(String jsonToMutate, String expectedResponseId, MockMvc mockMvc)
        throws Exception {
        mockMvc.perform(
            post(URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonToMutate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[*]", hasSize(1)))
            .andExpect(jsonPath("$.records[0].id", is(expectedResponseId)));
    }
}
