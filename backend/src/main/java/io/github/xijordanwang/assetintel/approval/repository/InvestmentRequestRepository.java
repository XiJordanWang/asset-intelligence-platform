package io.github.xijordanwang.assetintel.approval.repository;

import io.github.xijordanwang.assetintel.approval.entity.InvestmentRequest;
import io.github.xijordanwang.assetintel.approval.entity.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvestmentRequestRepository extends JpaRepository<InvestmentRequest, Long> {

    @EntityGraph(attributePaths = {"site", "steps"})
    Optional<InvestmentRequest> findByReference(String reference);

    @EntityGraph(attributePaths = {"site"})
    List<InvestmentRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    @EntityGraph(attributePaths = {"site"})
    List<InvestmentRequest> findAllByOrderByCreatedAtDesc();

    /** Capex already committed at a site in a period: requests that are pending, approved or capitalised. */
    @Query("""
            select coalesce(sum(r.amount), 0) from InvestmentRequest r
            where r.site.id = :siteId and r.status in :statuses
              and r.createdAt >= :from and r.createdAt < :to
            """)
    BigDecimal committedAmount(@Param("siteId") Long siteId,
                               @Param("statuses") Collection<RequestStatus> statuses,
                               @Param("from") Instant from,
                               @Param("to") Instant to);
}
