package br.com.nuvia.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<UserAccount, Long> {
    @Query("""
        select a from UserAccount a
        where (:filter = '' or lower(a.fullName) like :filter escape '!'
               or lower(a.email) like :filter escape '!'
               or a.phone like :filter escape '!')
        """)
    Page<UserAccount> findForAdmin(@Param("filter") String filter, Pageable pageable);
    long countByAdminTrue();
    long countByCreatedAtGreaterThanEqual(Instant since);

    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByPhone(String phone);
    @Query("select a.id from UserAccount a where a.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);
    @Query("select a.id from UserAccount a where a.phone = :phone")
    Optional<Long> findIdByPhone(@Param("phone") String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAccount a where a.id = :id")
    Optional<UserAccount> lockById(@Param("id") Long id);
}
