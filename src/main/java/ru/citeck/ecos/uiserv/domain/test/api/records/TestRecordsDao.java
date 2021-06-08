package ru.citeck.ecos.uiserv.domain.test.api.records;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TestRecordsDao extends AbstractRecordsDao
    implements RecordAttsDao, RecordsQueryDao, RecordMutateDtoDao<TestRecordsDao.TestDto>, RecordDeleteDao {

    private List<TestDto> list = new ArrayList<>();

    @PostConstruct
    public void init() {
        list.add(new TestDto("1", "Иван", 11));
        list.add(new TestDto("2", "Игорь", 22));
        list.add(new TestDto("3", "Перт", 33));
        list.add(new TestDto(" ", "Кыфыс", 44));
    }

    @NotNull
    @Override
    public String getId() {
        return "test-id";
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String id) {
        return list.stream()
            .filter(i -> i.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {
        if (!PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {
            return null;
        }

        Predicate predicate = recordsQuery.getQuery(Predicate.class);
        List <TestDto> result = predicateService.filter(list, predicate);

        return new RecsQueryRes<>(result);
    }

    @Override
    public TestDto getRecToMutate(@NotNull String id) {
        return new TestDto(list.stream()
            .filter(i -> i.getId().equals(id))
            .findFirst()
            .orElse(new TestDto("-1", "Empty", -1)));
    }

    @NotNull
    @Override
    public String saveMutatedRec(TestDto testDto) {
         TestDto dto = list.stream()
            .filter(i -> i.getId().equals(testDto.getId()))
            .findFirst()
            .orElse(null);

         if (dto != null) {
            dto.setName(testDto.getName());
            dto.setNum(testDto.getNum());
         }
         return testDto.getId();
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String localId) {
        list = list.stream()
            .filter(i -> !i.getId().equals(localId))
            .collect(Collectors.toList());

        return DelStatus.OK;
    }

    @Data
    @AllArgsConstructor
    public static class TestDto {
        private String id;
        private String name;
        private int num;

        public TestDto(TestDto dto) {
            this.id = dto.getId();
            this.name = dto.getName();
            this.num = dto.getNum();
        }
    }
}
