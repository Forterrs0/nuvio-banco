package br.com.nuvia.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findByPublicId(String publicId);
    Optional<LedgerEntry> findByInitiatorIdAndIdempotencyKey(Long initiatorId, String idempotencyKey);

    @Query("select t from LedgerEntry t where t.sender.id = :id or t.recipient.id = :id order by t.createdAt desc, t.id desc")
    Page<LedgerEntry> findAccountStatement(@Param("id") Long accountId, Pageable pageable);

    @Query("select t from LedgerEntry t where (t.sender.id = :id or t.recipient.id = :id) and t.createdAt >= :from order by t.createdAt desc")
    List<LedgerEntry> findSince(@Param("id") Long accountId, @Param("from") Instant from);
}
