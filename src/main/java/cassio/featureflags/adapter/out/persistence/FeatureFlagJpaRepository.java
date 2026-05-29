package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.adapter.out.persistence.entity.FeatureFlagEntity;
import cassio.featureflags.domain.FlagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface FeatureFlagJpaRepository extends JpaRepository<FeatureFlagEntity, Long> {

    Optional<FeatureFlagEntity> findByFlagName(String flagName);

    boolean existsByFlagName(String flagName);

    List<FeatureFlagEntity> findByFlagNameIn(List<String> flagNames);

    @Query("""
            SELECT DISTINCT f FROM FeatureFlagEntity f
            WHERE (:service IS NULL OR f.serviceName = :service)
              AND (:type    IS NULL OR f.type         = :type)
              AND (:search  IS NULL OR f.flagName LIKE %:search%)
              AND (:env     IS NULL OR :env MEMBER OF f.envs)
            """)
    List<FeatureFlagEntity> findAllWithFilters(
            @Param("service") String service,
            @Param("env") String env,
            @Param("type") FlagType type,
            @Param("search") String search);

    @Query("SELECT DISTINCT f.serviceName FROM FeatureFlagEntity f ORDER BY f.serviceName")
    List<String> findDistinctServiceNames();
}
