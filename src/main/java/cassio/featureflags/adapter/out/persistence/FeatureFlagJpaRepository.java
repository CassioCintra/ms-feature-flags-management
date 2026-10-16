package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.adapter.out.persistence.entity.FeatureFlagEntity;
import cassio.featureflags.domain.FlagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FeatureFlagJpaRepository extends JpaRepository<FeatureFlagEntity, Long> {

    Optional<FeatureFlagEntity> findByWorkspaceIdAndFlagName(UUID workspaceId, String flagName);

    boolean existsByWorkspaceIdAndFlagName(UUID workspaceId, String flagName);

    List<FeatureFlagEntity> findByWorkspaceIdAndFlagNameIn(UUID workspaceId, List<String> flagNames);

    @Query("""
            SELECT DISTINCT f FROM FeatureFlagEntity f
            LEFT JOIN f.environments e
            WHERE f.workspaceId = :workspaceId
              AND (:service IS NULL OR f.serviceName = :service)
              AND (:type    IS NULL OR f.type         = :type)
              AND (:search  IS NULL OR f.flagName LIKE %:search%)
              AND (:env     IS NULL OR KEY(e) = :env)
            """)
    List<FeatureFlagEntity> findAllWithFilters(
            @Param("workspaceId") UUID workspaceId,
            @Param("service") String service,
            @Param("env") String env,
            @Param("type") FlagType type,
            @Param("search") String search);

    @Query("SELECT DISTINCT f.serviceName FROM FeatureFlagEntity f WHERE f.workspaceId = :workspaceId ORDER BY f.serviceName")
    List<String> findDistinctServiceNames(@Param("workspaceId") UUID workspaceId);
}
