package io.github.xijordanwang.assetintel.stocktake.entity;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The part of a campaign one site's coordinator is responsible for. */
@Entity
@Table(name = "stocktake_task")
public class StocktakeTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private StocktakeCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(nullable = false)
    private String coordinator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.OPEN;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StocktakeLine> lines = new ArrayList<>();

    protected StocktakeTask() {
    }

    StocktakeTask(StocktakeCampaign campaign, Site site, List<Asset> assetsToCount) {
        this.campaign = campaign;
        this.site = site;
        this.coordinator = site.getCoordinator();
        assetsToCount.forEach(a -> lines.add(new StocktakeLine(this, a)));
    }

    public void record(String assetTag, CountResult result, String note, String countedBy, Instant at) {
        if (status != TaskStatus.OPEN) {
            throw new BusinessRuleException("The count for " + site.getCode() + " has already been submitted");
        }
        if (!countedBy.equalsIgnoreCase(coordinator)) {
            throw new BusinessRuleException("Only " + coordinator + " can record counts for " + site.getCode());
        }
        if (result == CountResult.PENDING) {
            throw new BusinessRuleException("A count must be FOUND, DAMAGED or MISSING");
        }
        StocktakeLine line = lines.stream().filter(l -> l.getAsset().getTag().equals(assetTag)).findFirst()
                .orElseThrow(() -> new BusinessRuleException(assetTag + " is not on the count list for " + site.getCode()));
        line.record(result, note, countedBy, at);
    }

    public void submit(String by, Instant at) {
        if (!by.equalsIgnoreCase(coordinator)) {
            throw new BusinessRuleException("Only " + coordinator + " can submit the count for " + site.getCode());
        }
        long pending = lines.stream().filter(l -> l.getResult() == CountResult.PENDING).count();
        if (pending > 0) {
            throw new BusinessRuleException(pending + " assets at " + site.getCode() + " have not been counted yet");
        }
        status = TaskStatus.SUBMITTED;
        submittedAt = at;
    }

    public long count(CountResult result) {
        return lines.stream().filter(l -> l.getResult() == result).count();
    }

    public Site getSite() { return site; }
    public String getCoordinator() { return coordinator; }
    public TaskStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public List<StocktakeLine> getLines() { return List.copyOf(lines); }
}
