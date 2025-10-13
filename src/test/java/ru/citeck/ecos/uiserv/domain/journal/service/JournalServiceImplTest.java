package ru.citeck.ecos.uiserv.domain.journal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactoryImpl;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
public class JournalServiceImplTest {

    private JournalServiceImpl journalService;

    @MockitoBean
    private RecordsService recordsService;

    @MockitoSpyBean
    private JournalMapper journalMapper;

    @MockitoBean
    private JournalRepository journalRepository;

    @BeforeEach
    void setUp() {
        journalService = new JournalServiceImpl(journalRepository, journalMapper, new JpaSearchConverterFactoryImpl());
    }

    /*@Test
    void searchJournalByTypeRef_ByFirstRequest() {

        //  arrange

        EntityRef typeRef = EntityRef.create("emodel", "type", "test_type_id");
        EntityRef journalRef = EntityRef.create("uiserv", "journal", "test_journal_id");

        JournalServiceImpl.TypeJournalMeta typeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(journalRef);

        Mockito.when(recordsService.getMeta(typeRef, JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(typeWithJournalMeta);

        JournalDto journalDto = new JournalDto();
        journalDto.setId("test_journal_id");

        JournalEntity journalEntity = journalMapper.dtoToEntity(journalDto);

        Mockito.when(journalRepository.findByExtId(journalRef.getId()))
            .thenReturn(Optional.of(journalEntity));

        //  act

        JournalDto resultJournalDto = journalService.getJournalByTypeRef(typeRef);

        //  assert

        Assert.assertEquals(journalDto, resultJournalDto);
    }

    @Test
    void searchJournalByTypeRef_WithParentsIteration() {

        //  arrange

        EntityRef journalRef = EntityRef.create("uiserv", "journal", "test_journal_id");

        List<EntityRef> parentsRefs = Collections.singletonList(
            EntityRef.create("emodel", "type", "parent_id_1")
        );

        JournalServiceImpl.TypeJournalMeta typeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(null);
        typeWithJournalMeta.setParentsRefs(parentsRefs);

        EntityRef typeRef = EntityRef.create("emodel", "type", "test_type_id");

        Mockito.when(recordsService.getMeta(typeRef, JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(typeWithJournalMeta);

        JournalServiceImpl.TypeJournalMeta parentTypeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(journalRef);
        typeWithJournalMeta.setParentsRefs(null);

        Mockito.when(recordsService.getMeta(parentsRefs.get(0), JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(parentTypeWithJournalMeta);

        JournalDto journalDto = new JournalDto();
        journalDto.setId("test_journal_id");

        JournalEntity journalEntity = journalMapper.dtoToEntity(journalDto);

        Mockito.when(journalRepository.findByExtId(journalRef.getId()))
            .thenReturn(Optional.of(journalEntity));

        //  act

        JournalDto resultJournalDto = journalService.getJournalByTypeRef(typeRef);

        //  assert

        Assert.assertEquals(journalDto, resultJournalDto);
    }

    @Test
    void searchJournalByTypeRef_WithSameTypeRef() {

        //  arrange

        EntityRef typeRef = EntityRef.create("emodel", "type", "test_type_id");

        JournalDto journalDto = new JournalDto();
        journalDto.setId("test_journal_id");

        JournalEntity journalEntity = journalMapper.dtoToEntity(journalDto);

        Mockito.when(journalRepository.findAllByTypeRef(typeRef.toString()))
            .thenReturn(Collections.singleton(journalEntity));

        JournalServiceImpl.TypeJournalMeta typeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(null);
        typeWithJournalMeta.setParentsRefs(null);

        Mockito.when(recordsService.getMeta(typeRef, JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(typeWithJournalMeta);

        //  act

        JournalDto resultJournalDto = journalService.getJournalByTypeRef(typeRef);

        //  assert

        Assert.assertEquals(journalDto, resultJournalDto);
    }

    @Test
    void searchJournalByTypeRef_WithSameParentTypeRef() {

        //  arrange

        EntityRef typeRef = EntityRef.create("emodel", "type", "test_type_id");

        JournalDto journalDto = new JournalDto();
        journalDto.setId("test_journal_id");

        List<EntityRef> parentsRefs = Collections.singletonList(
            EntityRef.create("emodel", "type", "parent_id_1")
        );

        JournalEntity journalEntity = journalMapper.dtoToEntity(journalDto);

        Mockito.when(journalRepository.findAllByTypeRef(parentsRefs.get(0).toString()))
            .thenReturn(Collections.singleton(journalEntity));

        JournalServiceImpl.TypeJournalMeta typeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(null);
        typeWithJournalMeta.setParentsRefs(parentsRefs);

        Mockito.when(recordsService.getMeta(typeRef, JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(typeWithJournalMeta);

        JournalServiceImpl.TypeJournalMeta parentTypeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        parentTypeWithJournalMeta.setJournalRef(null);
        parentTypeWithJournalMeta.setParentsRefs(null);

        Mockito.when(recordsService.getMeta(parentsRefs.get(0), JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(parentTypeWithJournalMeta);

        //  act

        JournalDto resultJournalDto = journalService.getJournalByTypeRef(typeRef);

        //  assert

        Assert.assertEquals(journalDto, resultJournalDto);
    }

    @Test
    void searchJournalByTypeRef_WithoutResult() {

        //  arrange

        EntityRef typeRef = EntityRef.create("emodel", "type", "test_type_id");

        JournalDto journalDto = new JournalDto();
        journalDto.setId("test_journal_id");

        JournalServiceImpl.TypeJournalMeta typeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(null);
        typeWithJournalMeta.setParentsRefs(null);

        Mockito.when(recordsService.getMeta(typeRef, JournalServiceImpl.TypeJournalMeta.class))
            .thenReturn(typeWithJournalMeta);

        //  act

        JournalDto resultJournalDto = journalService.getJournalByTypeRef(typeRef);

        //  assert

        Assert.assertNull(resultJournalDto);
    }*/
}
