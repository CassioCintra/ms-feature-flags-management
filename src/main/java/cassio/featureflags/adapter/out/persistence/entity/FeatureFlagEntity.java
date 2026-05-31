package cassio.featureflags.adapter.out.persistence.entity;

import cassio.featureflags.domain.FeatureFlag;
import cassio.featureflags.domain.FlagType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_name", nullable = false, unique = true)
    private String flagName;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagType type;

    @Column
    private Integer rollout;

    @ElementCollection
    @CollectionTable(name = "flag_environments", joinColumns = @JoinColumn(name = "flag_id"))
    @MapKeyColumn(name = "env_name")
    @Column(name = "enabled")
    private Map<String, Boolean> environments;

    @ElementCollection
    @CollectionTable(name = "flag_tags", joinColumns = @JoinColumn(name = "flag_id"))
    @Column(name = "tag_name")
    private Set<String> tags;

    @Column
    private String owner;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

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
                .type(flag.getType() != null ? flag.getType() : FlagType.BOOLEAN)
                .rollout(flag.getRollout())
                .environments(flag.getEnvironments() != null ? new HashMap<>(flag.getEnvironments()) : new HashMap<>())
                .tags(flag.getTags() != null ? new HashSet<>(flag.getTags()) : new HashSet<>())
                .owner(flag.getOwner())
                .expiresAt(flag.getExpiresAt())
                .enabled(flag.isEnabled())
                .build();
    }

    public FeatureFlag toDomain() {
        return FeatureFlag.builder()
                .id(id)
                .flagName(flagName)
                .serviceName(serviceName)
                .type(type)
                .rollout(rollout)
                .environments(environments != null ? new HashMap<>(environments) : Map.of())
                .tags(tags != null ? new ArrayList<>(tags) : List.of())
                .owner(owner)
                .expiresAt(expiresAt)
                .enabled(enabled)
                .build();
    }
}
