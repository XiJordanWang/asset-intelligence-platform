package io.github.xijordanwang.assetintel.stocktake.dto;

import io.github.xijordanwang.assetintel.stocktake.entity.CampaignStatus;
import io.github.xijordanwang.assetintel.stocktake.entity.CountResult;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeCampaign;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeLine;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeTask;
import io.github.xijordanwang.assetintel.stocktake.entity.TaskStatus;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StocktakeDtos {

    private StocktakeDtos() {
    }

    public record PlanCampaign(
            @NotBlank @Size(max = 200) String title,
            String scopePlant,
            AssetCategory scopeCategory,
            @Min(1) @Max(100) int samplePercent,
            boolean photoRequired,
            @NotNull LocalDate dueDate,
            String createdBy) { // ignored: the signed-in user plans the stocktake
    }

    public record RecordCount(@NotBlank String assetTag, @NotNull CountResult result,
                              @Size(max = 500) String note, String countedBy) { // countedBy ignored
    }

    public record SubmitSite(String submittedBy) { // ignored: the signed-in coordinator submits
    }

    public record SiteProgress(String siteCode, String siteName, String coordinator, TaskStatus status,
                               long total, long counted, long found, long damaged, long missing) {
        public static SiteProgress of(StocktakeTask t) {
            long total = t.getLines().size();
            return new SiteProgress(t.getSite().getCode(), t.getSite().getName(), t.getCoordinator(), t.getStatus(),
                    total, total - t.count(CountResult.PENDING), t.count(CountResult.FOUND),
                    t.count(CountResult.DAMAGED), t.count(CountResult.MISSING));
        }
    }

    public record CampaignView(String reference, String title, String scopePlant, AssetCategory scopeCategory,
                               int samplePercent, boolean photoRequired, CampaignStatus status, LocalDate dueDate,
                               String createdBy, Instant createdAt, Instant startedAt, Instant completedAt,
                               long total, long counted, long missing, List<SiteProgress> sites) {
        public static CampaignView of(StocktakeCampaign c) {
            List<SiteProgress> sites = c.getTasks().stream().map(SiteProgress::of).toList();
            return new CampaignView(c.getReference(), c.getTitle(), c.getScopePlant(), c.getScopeCategory(),
                    c.getSamplePercent(), c.isPhotoRequired(), c.getStatus(), c.getDueDate(), c.getCreatedBy(),
                    c.getCreatedAt(), c.getStartedAt(), c.getCompletedAt(),
                    sites.stream().mapToLong(SiteProgress::total).sum(),
                    sites.stream().mapToLong(SiteProgress::counted).sum(),
                    sites.stream().mapToLong(SiteProgress::missing).sum(),
                    sites);
        }
    }

    public record LineView(String assetTag, String assetName, AssetCategory category, String custodian,
                           CountResult result, String note, String countedBy, Instant countedAt) {
        public static LineView of(StocktakeLine l) {
            return new LineView(l.getAsset().getTag(), l.getAsset().getName(), l.getAsset().getCategory(),
                    l.getAsset().getCustodian(), l.getResult(), l.getNote(), l.getCountedBy(), l.getCountedAt());
        }
    }

    public record SiteTaskView(String campaignReference, CampaignStatus campaignStatus, SiteProgress progress,
                               List<LineView> lines) {
    }

    public record CompletionView(CampaignView campaign, int verified, List<String> draftDisposals) {
    }
}
