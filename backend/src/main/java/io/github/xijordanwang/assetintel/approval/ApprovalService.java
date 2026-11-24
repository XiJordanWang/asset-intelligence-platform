package io.github.xijordanwang.assetintel.approval;

import io.github.xijordanwang.assetintel.asset.Asset;
import io.github.xijordanwang.assetintel.asset.AssetCategory;
import io.github.xijordanwang.assetintel.asset.AssetRepository;
import io.github.xijordanwang.assetintel.asset.AssetStatus;
import io.github.xijordanwang.assetintel.common.BusinessRuleException;
import io.github.xijordanwang.assetintel.common.NotFoundException;
import io.github.xijordanwang.assetintel.site.Site;
import io.github.xijordanwang.assetintel.site.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;

@Service
@Transactional
public class ApprovalService {

    static final ZoneId NZ = ZoneId.of("Pacific/Auckland");

    private final InvestmentRequestRepository requests;
    private final SiteRepository sites;
    private final AssetRepository assets;
    private final Clock clock;

    public ApprovalService(InvestmentRequestRepository requests, SiteRepository sites,
                           AssetRepository assets, Clock clock) {
        this.requests = requests;
        this.sites = sites;
        this.assets = assets;
        this.clock = clock;
    }

    public InvestmentRequest createDraft(String title, String justification, AssetCategory category,
                                        BigDecimal amount, String siteCode, String requestedBy) {
        Site site = sites.findByCode(siteCode)
                .orElseThrow(() -> new NotFoundException("No site with code " + siteCode));
        Instant now = clock.instant();
        int year = LocalDate.ofInstant(now, NZ).getYear();
        String reference = "IR-%d-%05d".formatted(year, requests.count() + 1);
        return requests.save(new InvestmentRequest(reference, title, justification, category, amount,
                site, requestedBy, now));
    }

    /** Submits a draft for approval after checking it fits in the site's remaining capex budget for the year. */
    public InvestmentRequest submit(String reference) {
        InvestmentRequest request = get(reference);
        BigDecimal remaining = remainingBudget(request.getSite());
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new BusinessRuleException("%s needs NZD %s but site %s has NZD %s of capex budget left this year"
                    .formatted(reference, request.getAmount().toPlainString(), request.getSite().getCode(),
                            remaining.toPlainString()));
        }
        request.submit(clock.instant());
        return request;
    }

    public InvestmentRequest decide(String reference, ApproverRole role, Decision decision,
                                    String decidedBy, String comment) {
        InvestmentRequest request = get(reference);
        request.decide(role, decision, decidedBy, comment, clock.instant());
        return request;
    }

    /** Turns an approved request into an asset in the register. Construction stays PENDING until finished. */
    public InvestmentRequest capitalise(String reference) {
        InvestmentRequest request = get(reference);
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessRuleException("Cannot capitalise " + reference + " while it is " + request.getStatus());
        }
        boolean underConstruction = request.getCategory() == AssetCategory.CONSTRUCTION_IN_PROGRESS;
        Asset asset = assets.save(new Asset(
                "CAP-" + reference,
                request.getTitle(),
                request.getCategory(),
                underConstruction ? AssetStatus.PENDING : AssetStatus.IN_SERVICE,
                request.getSite(),
                null,
                request.getAmount(),
                underConstruction ? null : LocalDate.now(clock.withZone(NZ))));
        request.capitalise(asset, clock.instant());
        return request;
    }

    @Transactional(readOnly = true)
    public InvestmentRequest get(String reference) {
        return requests.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("No investment request " + reference));
    }

    @Transactional(readOnly = true)
    public BigDecimal remainingBudget(Site site) {
        int year = LocalDate.now(clock.withZone(NZ)).getYear();
        Instant from = LocalDate.of(year, 1, 1).atStartOfDay(NZ).toInstant();
        Instant to = LocalDate.of(year + 1, 1, 1).atStartOfDay(NZ).toInstant();
        BigDecimal committed = requests.committedAmount(site.getId(),
                EnumSet.of(RequestStatus.PENDING_APPROVAL, RequestStatus.APPROVED, RequestStatus.CAPITALISED),
                from, to);
        return site.getAnnualCapexBudget().subtract(committed);
    }
}
