package ru.citeck.ecos.uiserv.domain.journal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl;

@ExtendWith(SpringExtension.class)
public class JournalServiceImplTest {

    private JournalServiceImpl journalService;

    @MockBean
    private RecordsService recordsService;

    @SpyBean
    private JournalMapper journalMapper;

    @MockBean
    private JournalRepository journalRepository;

    @BeforeEach
    void setUp() {
        journalService = new JournalServiceImpl(journalRepository, journalMapper, recordsService);
    }

    /*@Test
    void searchJournalByTypeRef_ByFirstRequest() {

        //  arrange

        RecordRef typeRef = RecordRef.create("emodel", "type", "test_type_id");
        RecordRef journalRef = RecordRef.create("uiserv", "journal", "test_journal_id");

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

        RecordRef journalRef = RecordRef.create("uiserv", "journal", "test_journal_id");

        List<RecordRef> parentsRefs = Collections.singletonList(
            RecordRef.create("emodel", "type", "parent_id_1")
        );

        JournalServiceImpl.TypeJournalMeta typeWithJournalMeta = new JournalServiceImpl.TypeJournalMeta();
        typeWithJournalMeta.setJournalRef(null);
        typeWithJournalMeta.setParentsRefs(parentsRefs);

        RecordRef typeRef = RecordRef.create("emodel", "type", "test_type_id");

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

        RecordRef typeRef = RecordRef.create("emodel", "type", "test_type_id");

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

        RecordRef typeRef = RecordRef.create("emodel", "type", "test_type_id");

        JournalDto journalDto = new JournalDto();
        journalDto.setId("test_journal_id");

        List<RecordRef> parentsRefs = Collections.singletonList(
            RecordRef.create("emodel", "type", "parent_id_1")
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

        RecordRef typeRef = RecordRef.create("emodel", "type", "test_type_id");

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
