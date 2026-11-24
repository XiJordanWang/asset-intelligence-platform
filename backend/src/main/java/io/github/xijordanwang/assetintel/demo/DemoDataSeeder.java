package io.github.xijordanwang.assetintel.demo;

import io.github.xijordanwang.assetintel.approval.ApprovalService;
import io.github.xijordanwang.assetintel.approval.ApproverRole;
import io.github.xijordanwang.assetintel.approval.Decision;
import io.github.xijordanwang.assetintel.approval.InvestmentRequest;
import io.github.xijordanwang.assetintel.approval.RequestStatus;
import io.github.xijordanwang.assetintel.asset.AssetCategory;
import io.github.xijordanwang.assetintel.asset.AssetRepository;
import io.github.xijordanwang.assetintel.site.Site;
import io.github.xijordanwang.assetintel.site.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/** Fills an empty database with the fictional company, using the real services so the data obeys the same rules. */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[] REQUESTERS = {"aroha.engineer", "ben.maintenance", "mere.it", "sam.logistics", "lucy.facilities"};
    private static final String[][] REQUEST_IDEAS = {
            {"Replace end-of-life robotic welding cell", "PLANT_MACHINERY"},
            {"Upgrade plant Wi-Fi and network switches", "IT_EQUIPMENT"},
            {"Two electric forklifts to replace diesel units", "VEHICLE"},
            {"Rooftop solar array", "CONSTRUCTION_IN_PROGRESS"},
            {"Laser cutter for prototype workshop", "PLANT_MACHINERY"},
            {"Laptop refresh for engineering team", "IT_EQUIPMENT"},
            {"Training room fit-out", "FURNITURE"},
            {"Second injection moulding machine", "PLANT_MACHINERY"},
            {"Delivery van for regional service centre", "VEHICLE"},
            {"Warehouse racking extension", "FURNITURE"}};

    private final SiteRepository sites;
    private final AssetRepository assets;
    private final ApprovalService approvals;
    private final Clock clock;

    public DemoDataSeeder(SiteRepository sites, AssetRepository assets, ApprovalService approvals, Clock clock) {
        this.sites = sites;
        this.assets = assets;
        this.approvals = approvals;
        this.clock = clock;
    }

    @Transactional
    public boolean seedIfEmpty(DemoProperties props) {
        if (sites.count() > 0) {
            return false;
        }
        MockDataGenerator generator = new MockDataGenerator(props.randomSeed());
        List<Site> savedSites = sites.saveAll(generator.sites());
        assets.saveAll(generator.assets(savedSites, props.assetCount(), LocalDate.now(clock)));
        seedRequests(savedSites, props.requestCount(), generator.random());
        log.info("Seeded demo data: {} sites, {} assets, {} investment requests",
                savedSites.size(), props.assetCount(), props.requestCount());
        return true;
    }

    private void seedRequests(List<Site> savedSites, int count, Random random) {
        for (int i = 0; i < count; i++) {
            String[] idea = REQUEST_IDEAS[random.nextInt(REQUEST_IDEAS.length)];
            Site site = savedSites.get(random.nextInt(savedSites.size()));
            double scale = random.nextDouble();
            BigDecimal amount = BigDecimal.valueOf(scale < 0.5 ? 5_000 + scale * 80_000
                    : scale < 0.85 ? 60_000 + scale * 300_000 : 500_000 + scale * 400_000)
                    .setScale(2, RoundingMode.HALF_UP);
            InvestmentRequest r = approvals.createDraft(idea[0], "Demo request generated for the portfolio demo.",
                    AssetCategory.valueOf(idea[1]), amount, site.getCode(),
                    REQUESTERS[random.nextInt(REQUESTERS.length)]);

            double stage = random.nextDouble();
            if (stage < 0.15 || amount.compareTo(approvals.remainingBudget(site)) > 0) {
                continue; // leave as draft
            }
            approvals.submit(r.getReference());
            // Walk the approval chain part of the way, sometimes rejecting.
            while (r.getStatus() == RequestStatus.PENDING_APPROVAL && random.nextDouble() < 0.75) {
                ApproverRole role = r.nextPendingStep().orElseThrow().getApproverRole();
                Decision decision = random.nextDouble() < 0.12 ? Decision.REJECTED : Decision.APPROVED;
                approvals.decide(r.getReference(), role, decision, approverName(role),
                        decision == Decision.REJECTED ? "Please revisit the business case." : "Looks good.");
            }
            if (r.getStatus() == RequestStatus.APPROVED && random.nextDouble() < 0.5) {
                approvals.capitalise(r.getReference());
            }
        }
    }

    private static String approverName(ApproverRole role) {
        return switch (role) {
            case PLANT_MANAGER -> "tama.plantmanager";
            case FINANCE_CONTROLLER -> "grace.finance";
            case CFO -> "hemi.cfo";
        };
    }
}
