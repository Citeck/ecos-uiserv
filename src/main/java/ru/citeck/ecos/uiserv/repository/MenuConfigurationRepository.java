package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.MenuConfigurationEntity;

import java.util.Optional;

@Repository
public interface MenuConfigurationRepository extends JpaRepository<MenuConfigurationEntity, Long> {
    Optional<MenuConfigurationEntity> findByExtId(String extId);
}
