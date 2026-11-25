package io.github.xijordanwang.assetintel.stocktake.entity;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A physical verification exercise: PLANNED -> IN_PROGRESS -> COMPLETED (or CANCELLED).
 * Starting it fans out one task per site in scope, each with the (optionally sampled) assets to count.
 */
@Entity
@Table(name = "stocktake_campaign")
public class StocktakeCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String title;

    /** Null means every plant. */
    @Column(name = "scope_plant")
    private String scopePlant;

    /** Null means every asset class. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_category")
    private AssetCategory scopeCategory;

    /** 100 is a full count; lower values count a random sample at each site. */
    @Column(name = "sample_percent", nullable = false)
    private int samplePercent;

    @Column(name = "photo_required", nullable = false)
    private boolean photoRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.PLANNED;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<StocktakeTask> tasks = new ArrayList<>();

    protected StocktakeCampaign() {
    }

    public StocktakeCampaign(String reference, String title, String scopePlant, AssetCategory scopeCategory,
                             int samplePercent, boolean photoRequired, LocalDate dueDate, String createdBy, Instant now) {
        if (samplePercent < 1 || samplePercent > 100) {
            throw new BusinessRuleException("Sample percent must be between 1 and 100");
        }
        this.reference = reference;
        this.title = title;
        this.scopePlant = scopePlant;
        this.scopeCategory = scopeCategory;
        this.samplePercent = samplePercent;
        this.photoRequired = photoRequired;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public boolean inScope(Asset asset) {
        return (scopePlant == null || scopePlant.equals(asset.getSite().getPlantCode()))
                && (scopeCategory == null || scopeCategory == asset.getCategory());
    }

    public void start(Map<Site, List<Asset>> countListBySite, Instant now) {
        requireStatus(CampaignStatus.PLANNED, "start");
        if (countListBySite.isEmpty()) {
            throw new BusinessRuleException("No assets are in scope for " + reference);
        }
        countListBySite.forEach((site, assets) -> tasks.add(new StocktakeTask(this, site, assets)));
        status = CampaignStatus.IN_PROGRESS;
        startedAt = now;
    }

    public StocktakeTask task(String siteCode) {
        return tasks.stream().filter(t -> t.getSite().getCode().equals(siteCode)).findFirst()
                .orElseThrow(() -> new BusinessRuleException(siteCode + " is not part of " + reference));
    }

    public void requireInProgress() {
        requireStatus(CampaignStatus.IN_PROGRESS, "count for");
    }

    public void complete(Instant now) {
        requireStatus(CampaignStatus.IN_PROGRESS, "complete");
        long open = tasks.stream().filter(t -> t.getStatus() != TaskStatus.SUBMITTED).count();
        if (open > 0) {
            throw new BusinessRuleException(open + " sites have not submitted their count yet");
        }
        status = CampaignStatus.COMPLETED;
        completedAt = now;
    }

    public void cancel() {
        if (status == CampaignStatus.COMPLETED || status == CampaignStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot cancel " + reference + " while it is " + status);
        }
        status = CampaignStatus.CANCELLED;
    }

    private void requireStatus(CampaignStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("Cannot " + action + " " + reference + " while it is " + status);
        }
    }

    public String getReference() { return reference; }
    public String getTitle() { return title; }
    public String getScopePlant() { return scopePlant; }
    public AssetCategory getScopeCategory() { return scopeCategory; }
    public int getSamplePercent() { return samplePercent; }
    public boolean isPhotoRequired() { return photoRequired; }
    public CampaignStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<StocktakeTask> getTasks() { return List.copyOf(tasks); }
}
