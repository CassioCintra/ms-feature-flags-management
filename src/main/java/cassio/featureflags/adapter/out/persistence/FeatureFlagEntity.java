package cassio.featureflags.adapter.out.persistence;

import cassio.featureflags.domain.FeatureFlag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feature_flags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"flag_name", "service_name", "environment_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_name", nullable = false)
    private String flagName;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "environment_name", nullable = false)
    private String environmentName;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static FeatureFlagEntity from(FeatureFlag flag) {
        return FeatureFlagEntity.builder()
                .id(flag.getId())
                .flagName(flag.getFlagName())
                .serviceName(flag.getServiceName())
                .environmentName(flag.getEnvironmentName())
                .enabled(flag.isEnabled())
                .build();
    }

    public FeatureFlag toDomain() {
        return FeatureFlag.builder()
                .id(id)
                .flagName(flagName)
                .serviceName(serviceName)
                .environmentName(environmentName)
                .enabled(enabled)
                .build();
    }
}
