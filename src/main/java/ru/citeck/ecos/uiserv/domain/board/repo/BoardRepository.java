package ru.citeck.ecos.uiserv.domain.board.repo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, Long>,
    JpaSpecificationExecutor<BoardEntity> {

    List<BoardEntity> findAllByTypeRef(String type, Sort sort);

    List<BoardEntity> findAllByTypeRefIn(List<String> types);

    Optional<BoardEntity> findByExtId(String extId);

    List<BoardEntity> findAllByJournalRef(String journal, Sort sort);
}
