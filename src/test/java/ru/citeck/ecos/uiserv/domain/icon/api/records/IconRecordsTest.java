package ru.citeck.ecos.uiserv.domain.icon.api.records;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.icon.dto.IconDto;
import ru.citeck.ecos.uiserv.domain.icon.repo.IconRepository;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
public class IconRecordsTest {

    @Autowired
    private RecordsService recordsService;
    @Autowired
    private IconService iconService;
    @Autowired
    private IconRepository iconRepository;

    @BeforeEach
    public void setUp() {
        iconRepository.deleteAll();
    }

    @Test
    public void queryReturnsUrlAttribute() {
        IconDto dto = createIcon("icon-url-test", "fa", "");

        RecsQueryRes<RecordAtts> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withQuery(DataValue.createObj())
                .build(),
            Map.of("url", "url")
        );

        assertEquals(1, result.getRecords().size());

        String url = result.getRecords().getFirst().getAtt("url").asText();
        assertTrue(url.startsWith("/gateway/uiserv/api/icon/"), "url should start with icon api path");
        assertTrue(url.contains("icon-url-test"), "url should contain icon id");
        assertTrue(url.contains("?cb="), "url should contain cache buster");
    }

    @Test
    public void queryPaginationFirstPage() {
        for (int i = 0; i < 5; i++) {
            createIcon("page-icon-" + i, "fa", "");
        }

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withQuery(DataValue.createObj())
                .withPage(QueryPage.create(b -> {
                    b.withMaxItems(2);
                    b.withSkipCount(0);
                    return null;
                }))
                .build()
        );

        assertEquals(5, result.getTotalCount());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void queryPaginationLastPage() {
        for (int i = 0; i < 5; i++) {
            createIcon("last-icon-" + i, "fa", "");
        }

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withQuery(DataValue.createObj())
                .withPage(QueryPage.create(b -> {
                    b.withMaxItems(2);
                    b.withSkipCount(4);
                    return null;
                }))
                .build()
        );

        assertEquals(5, result.getTotalCount());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    public void queryPaginationWithFamilyFilter() {
        for (int i = 0; i < 4; i++) {
            createIcon("fam-icon-" + i, "fa", "solid");
        }
        createIcon("other-icon", "img", "custom");

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withQuery(DataValue.createObj().set("family", "solid"))
                .withPage(QueryPage.create(b -> {
                    b.withMaxItems(2);
                    b.withSkipCount(0);
                    return null;
                }))
                .build()
        );

        assertEquals(4, result.getTotalCount());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void queryPaginationWithFamilyAndTypeFilter() {
        for (int i = 0; i < 3; i++) {
            createIcon("ft-icon-" + i, "fa", "solid");
        }
        createIcon("ft-other-1", "img", "solid");
        createIcon("ft-other-2", "fa", "regular");

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withQuery(DataValue.createObj().set("family", "solid").set("type", "fa"))
                .withPage(QueryPage.create(b -> {
                    b.withMaxItems(2);
                    b.withSkipCount(0);
                    return null;
                }))
                .build()
        );

        assertEquals(3, result.getTotalCount());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void queryWithoutPaginationReturnsAll() {
        for (int i = 0; i < 5; i++) {
            createIcon("all-icon-" + i, "fa", "");
        }

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withQuery(DataValue.createObj())
                .build()
        );

        assertEquals(5, result.getTotalCount());
        assertEquals(5, result.getRecords().size());
    }

    @Test
    public void queryByPredicateFiltersByFamily() {
        createIcon("pred-1", "fa", "solid");
        createIcon("pred-2", "fa", "solid");
        createIcon("pred-3", "img", "custom");

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withQuery(Predicates.eq("family", "solid"))
                .build()
        );

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void queryByPredicateFiltersByType() {
        createIcon("type-1", "fa", "solid");
        createIcon("type-2", "img", "solid");
        createIcon("type-3", "fa", "regular");

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withQuery(Predicates.eq("type", "fa"))
                .build()
        );

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void queryByPredicateWithPagination() {
        for (int i = 0; i < 5; i++) {
            createIcon("pred-page-" + i, "fa", "solid");
        }
        createIcon("pred-page-other", "img", "custom");

        RecsQueryRes<EntityRef> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withQuery(Predicates.eq("family", "solid"))
                .withPage(QueryPage.create(b -> {
                    b.withMaxItems(2);
                    b.withSkipCount(0);
                    return null;
                }))
                .build()
        );

        assertEquals(5, result.getTotalCount());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    public void queryByPredicateReturnsUrl() {
        createIcon("pred-url-icon", "fa", "solid");

        RecsQueryRes<RecordAtts> result = recordsService.query(
            RecordsQuery.create()
                .withSourceId(IconRecords.ID)
                .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                .withQuery(Predicates.eq("family", "solid"))
                .build(),
            Map.of("url", "url")
        );

        assertEquals(1, result.getRecords().size());
        String url = result.getRecords().getFirst().getAtt("url").asText();
        assertTrue(url.startsWith("/gateway/uiserv/api/icon/"));
        assertTrue(url.contains("pred-url-icon"));
    }

    @Test
    public void getAttsReturnsUrl() {
        IconDto saved = createIcon("atts-icon", "fa", "");

        String url = recordsService.getAtt(
            EntityRef.create("", IconRecords.ID, saved.getId()),
            "url"
        ).asText();

        assertTrue(url.startsWith("/gateway/uiserv/api/icon/"));
        assertTrue(url.contains("atts-icon"));
    }

    private IconDto createIcon(String id, String type, String family) {
        IconDto dto = new IconDto();
        dto.setId(id);
        dto.setType(type);
        dto.setFamily(family);
        dto.setData("data".getBytes());
        return iconService.save(dto);
    }
}
