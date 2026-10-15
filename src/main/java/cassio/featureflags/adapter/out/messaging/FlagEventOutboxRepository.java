package cassio.featureflags.adapter.out.messaging;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

interface FlagEventOutboxRepository extends JpaRepository<FlagEventOutboxEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<FlagEventOutboxEntity> findTop100ByOrderByIdAsc();
}
