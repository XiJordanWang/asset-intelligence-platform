package io.github.xijordanwang.assetintel.stocktake.service;

import io.github.xijordanwang.assetintel.stocktake.entity.CountResult;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeCampaign;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeLine;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeTask;
import io.github.xijordanwang.assetintel.stocktake.repository.StocktakeRepository;
import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.change.service.AssetChangeService;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.change.entity.DisposalReason;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.common.exception.NotFoundException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StocktakeService {

    private static final ZoneId NZ = ZoneId.of("Pacific/Auckland");

    private final StocktakeRepository campaigns;
    private final AssetRepository assets;
    private final AssetChangeService changes;
    private final Clock clock;

    public StocktakeService(StocktakeRepository campaigns, AssetRepository assets, AssetChangeService changes, Clock clock) {
        this.campaigns = campaigns;
        this.assets = assets;
        this.changes = changes;
        this.clock = clock;
    }

    public StocktakeCampaign plan(String title, String scopePlant, AssetCategory scopeCategory, int samplePercent,
                                  boolean photoRequired, LocalDate dueDate, String createdBy) {
        String reference = "ST-%d-%04d".formatted(LocalDate.now(clock.withZone(NZ)).getYear(), campaigns.count() + 1);
        return campaigns.save(new StocktakeCampaign(reference, title, blankToNull(scopePlant), scopeCategory,
                samplePercent, photoRequired, dueDate, createdBy, clock.instant()));
    }

    /**
     * Builds the count lists. Refuses to start while an in-scope asset is locked by an open change form
     * (its site or status could still move) or while a site in scope has no coordinator.
     */
    public StocktakeCampaign start(String reference) {
        StocktakeCampaign campaign = get(reference);
        List<Asset> inScope = assets.findAllActive().stream()
                .filter(a -> a.getCategory() != AssetCategory.LAND)
                .filter(campaign::inScope)
                .toList();

        List<String> locked = inScope.stream().filter(a -> a.getLockedByChange() != null)
                .map(a -> a.getTag() + " (" + a.getLockedByChange() + ")").toList();
        if (!locked.isEmpty()) {
            throw new BusinessRuleException("Finish or cancel the open forms first: " + preview(locked));
        }
        List<String> noCoordinator = inScope.stream().map(Asset::getSite).distinct()
                .filter(s -> s.getCoordinator() == null || s.getCoordinator().isBlank())
                .map(Site::getCode).sorted().toList();
        if (!noCoordinator.isEmpty()) {
            throw new BusinessRuleException("These sites need a coordinator first: " + preview(noCoordinator));
        }

        Random random = new Random(reference.hashCode());
        Map<Site, List<Asset>> bySite = inScope.stream().collect(Collectors.groupingBy(Asset::getSite,
                () -> new TreeMap<>(Comparator.comparing(Site::getCode)), Collectors.toList()));
        Map<Site, List<Asset>> countLists = new LinkedHashMap<>();
        bySite.forEach((site, list) -> countLists.put(site, sample(list, campaign.getSamplePercent(), random)));

        campaign.start(countLists, clock.instant());
        return campaign;
    }

    public StocktakeCampaign record(String reference, String siteCode, String assetTag, CountResult result,
                                    String note, String countedBy) {
        StocktakeCampaign campaign = get(reference);
        campaign.requireInProgress();
        campaign.task(siteCode).record(assetTag, result, note, countedBy, clock.instant());
        return campaign;
    }

    public StocktakeCampaign submitSite(String reference, String siteCode, String by) {
        StocktakeCampaign campaign = get(reference);
        campaign.requireInProgress();
        campaign.task(siteCode).submit(by, clock.instant());
        return campaign;
    }

    /**
     * Closes the campaign and feeds the results back into the register: found and damaged assets get today's
     * verification date (damaged ones go to maintenance), and each missing asset gets a draft LOST disposal.
     */
    public CompletionResult complete(String reference) {
        StocktakeCampaign campaign = get(reference);
        campaign.complete(clock.instant());
        LocalDate today = LocalDate.now(clock.withZone(NZ));
        List<String> disposals = new ArrayList<>();
        int verified = 0;
        for (StocktakeTask task : campaign.getTasks()) {
            for (StocktakeLine line : task.getLines()) {
                Asset asset = line.getAsset();
                switch (line.getResult()) {
                    case FOUND -> { asset.markVerified(today); verified++; }
                    case DAMAGED -> { asset.markVerified(today); asset.markDamaged(); verified++; }
                    case MISSING -> disposals.add(changes.createDraft(ChangeType.DISPOSAL, asset.getTag(),
                            task.getCoordinator(), "Not found in stocktake " + reference, null,
                            DisposalReason.LOST).getReference());
                    case PENDING -> throw new IllegalStateException("Submitted task with an uncounted line");
                }
            }
        }
        return new CompletionResult(campaign, verified, disposals);
    }

    public StocktakeCampaign cancel(String reference) {
        StocktakeCampaign campaign = get(reference);
        campaign.cancel();
        return campaign;
    }

    @Transactional(readOnly = true)
    public StocktakeCampaign get(String reference) {
        return campaigns.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("No stocktake " + reference));
    }

    public record CompletionResult(StocktakeCampaign campaign, int verified, List<String> draftDisposals) {
    }

    public static List<Asset> sample(List<Asset> assets, int percent, Random random) {
        if (percent >= 100) {
            return assets;
        }
        List<Asset> shuffled = new ArrayList<>(assets);
        Collections.shuffle(shuffled, random);
        int size = Math.max(1, (int) Math.ceil(assets.size() * percent / 100.0));
        return shuffled.subList(0, size);
    }

    private static String preview(List<String> items) {
        return items.size() <= 3 ? String.join(", ", items)
                : String.join(", ", items.subList(0, 3)) + " and " + (items.size() - 3) + " more";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
