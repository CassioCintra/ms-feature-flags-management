package cassio.featureflags.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface FeatureFlagJpaRepository extends JpaRepository<FeatureFlagEntity, Long> {

    List<FeatureFlagEntity> findByServiceNameAndEnvironmentName(String serviceName, String environmentName);

    boolean existsByFlagNameAndServiceNameAndEnvironmentName(String flagName, String serviceName, String environmentName);
}
