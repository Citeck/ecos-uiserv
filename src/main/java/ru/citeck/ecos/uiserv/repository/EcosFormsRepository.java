package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.EcosFormEntity;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface EcosFormsRepository extends JpaRepository<EcosFormEntity, Long> {

    List<EcosFormEntity> findAllByTypeRef(String type);

    List<EcosFormEntity> findAllByTypeRefIn(List<String> types);

    Optional<EcosFormEntity> findByExtId(String extId);

    Optional<EcosFormEntity> findByFormKey(String formKey);
}
