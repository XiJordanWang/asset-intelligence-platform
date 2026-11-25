package io.github.xijordanwang.assetintel.stocktake.entity;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stocktake_line")
public class StocktakeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private StocktakeTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CountResult result = CountResult.PENDING;

    private String note;

    @Column(name = "counted_by")
    private String countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    protected StocktakeLine() {
    }

    StocktakeLine(StocktakeTask task, Asset asset) {
        this.task = task;
        this.asset = asset;
    }

    public void record(CountResult result, String note, String countedBy, Instant at) {
        this.result = result;
        this.note = note;
        this.countedBy = countedBy;
        this.countedAt = at;
    }

    public Asset getAsset() { return asset; }
    public CountResult getResult() { return result; }
    public String getNote() { return note; }
    public String getCountedBy() { return countedBy; }
    public Instant getCountedAt() { return countedAt; }
}
