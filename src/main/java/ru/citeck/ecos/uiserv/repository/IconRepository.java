package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.citeck.ecos.uiserv.domain.IconEntity;

import java.util.List;
import java.util.Optional;

public interface IconRepository extends JpaRepository<IconEntity, Long> {

    List<IconEntity> findAllByType(String type);

    Optional<IconEntity> findByExtId(String extId);

    void deleteByExtId(String extId);
}
