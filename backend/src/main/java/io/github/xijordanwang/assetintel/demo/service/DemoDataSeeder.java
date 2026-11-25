package io.github.xijordanwang.assetintel.demo.service;

import io.github.xijordanwang.assetintel.demo.config.DemoProperties;
import io.github.xijordanwang.assetintel.approval.service.ApprovalService;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import io.github.xijordanwang.assetintel.approval.entity.InvestmentRequest;
import io.github.xijordanwang.assetintel.approval.entity.RequestStatus;
import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.service.AssetChangeService;
import io.github.xijordanwang.assetintel.change.entity.ChangeStatus;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.change.entity.DisposalReason;
import io.github.xijordanwang.assetintel.stocktake.entity.CountResult;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeCampaign;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeLine;
import io.github.xijordanwang.assetintel.stocktake.service.StocktakeService;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeTask;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.site.entity.Site;
import io.github.xijordanwang.assetintel.site.repository.SiteRepository;
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

    private static final String[] REQUESTERS = {"aroha.requester", "ben.maintenance", "mere.it", "sam.logistics", "lucy.facilities"};
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
    private final AssetChangeService changes;
    private final StocktakeService stocktakes;
    private final Clock clock;

    public DemoDataSeeder(SiteRepository sites, AssetRepository assets, ApprovalService approvals,
                          AssetChangeService changes, StocktakeService stocktakes, Clock clock) {
        this.sites = sites;
        this.assets = assets;
        this.approvals = approvals;
        this.changes = changes;
        this.stocktakes = stocktakes;
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
        seedStocktake(generator.random()); // before the change forms, which lock the assets they touch
        seedChanges(savedSites, props.requestCount() / 2, generator.random());
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
                approvals.decide(r.getReference(), role, decision, approverName(role, site.getPlantCode(), null),
                        decision == Decision.REJECTED ? "Please revisit the business case." : "Looks good.");
            }
            if (r.getStatus() == RequestStatus.APPROVED && random.nextDouble() < 0.5) {
                approvals.capitalise(r.getReference());
            }
        }
    }

    private void seedChanges(List<Site> savedSites, int count, Random random) {
        List<Asset> candidates = assets.findAllActive().stream()
                .filter(a -> a.getCategory() != AssetCategory.LAND && a.getCategory() != AssetCategory.BUILDING)
                .toList();
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            Asset asset = candidates.get(random.nextInt(candidates.size()));
            if (asset.getLockedByChange() != null) {
                continue;
            }
            String requester = REQUESTERS[random.nextInt(REQUESTERS.length)];
            AssetChange change;
            if (random.nextBoolean()) {
                Site target = savedSites.get(random.nextInt(savedSites.size()));
                if (target.getId().equals(asset.getSite().getId())) {
                    continue;
                }
                change = changes.createDraft(ChangeType.TRANSFER, asset.getTag(), requester,
                        "Equipment needed at " + target.getName(), target.getCode(), null);
            } else {
                DisposalReason reason = DisposalReason.values()[random.nextInt(DisposalReason.values().length)];
                change = changes.createDraft(ChangeType.DISPOSAL, asset.getTag(), requester,
                        "Demo disposal: " + reason.name().toLowerCase().replace('_', ' '), null, reason);
            }
            if (random.nextDouble() < 0.2) {
                continue; // leave as draft
            }
            changes.submit(change.getReference());
            while (change.getStatus() == ChangeStatus.PENDING_APPROVAL && random.nextDouble() < 0.6) {
                ApproverRole role = change.nextPendingStep().orElseThrow().getApproverRole();
                Decision decision = random.nextDouble() < 0.1 ? Decision.REJECTED : Decision.APPROVED;
                changes.decide(change.getReference(), role, decision, approverName(role,
                                asset.getSite().getPlantCode(),
                                change.getTargetSite() == null ? null : change.getTargetSite().getPlantCode()),
                        decision == Decision.REJECTED ? "Not this quarter." : "Approved.");
            }
        }
    }

    /** One stocktake in progress at the Hamilton plant, with some sites already counted. */
    private void seedStocktake(Random random) {
        StocktakeCampaign campaign = stocktakes.plan("Year-end IT equipment count, Hamilton plant", "HLZ",
                AssetCategory.IT_EQUIPMENT, 25, false, LocalDate.now(clock).plusWeeks(3), "grace.finance");
        stocktakes.start(campaign.getReference());
        List<StocktakeTask> tasks = campaign.getTasks();
        for (int i = 0; i < tasks.size() / 2; i++) {
            StocktakeTask task = tasks.get(i);
            for (StocktakeLine line : task.getLines()) {
                double r = random.nextDouble();
                CountResult result = r < 0.9 ? CountResult.FOUND : r < 0.96 ? CountResult.DAMAGED : CountResult.MISSING;
                stocktakes.record(campaign.getReference(), task.getSite().getCode(), line.getAsset().getTag(),
                        result, null, task.getCoordinator());
            }
            stocktakes.submitSite(campaign.getReference(), task.getSite().getCode(), task.getCoordinator());
        }
    }

    /** The demo user who holds each approval step, so seeded history matches who can sign in and act. */
    private static String approverName(ApproverRole role, String plant, String targetPlant) {
        return switch (role) {
            case PLANT_MANAGER -> plantManager(plant);
            case RECEIVING_PLANT_MANAGER -> plantManager(targetPlant);
            case FINANCE_CONTROLLER -> "grace.finance";
            case CFO -> "hemi.cfo";
        };
    }

    private static String plantManager(String plant) {
        return switch (plant) {
            case "AKL" -> "nikau.plantmanager";
            case "CHC" -> "ruby.plantmanager";
            case "WLG" -> "eli.plantmanager";
            default -> "tama.plantmanager";
        };
    }
}
