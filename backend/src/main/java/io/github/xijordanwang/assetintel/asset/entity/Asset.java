package io.github.xijordanwang.assetintel.asset.entity;

import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tag;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    private String supplier;

    @Column(name = "original_value", nullable = false)
    private BigDecimal originalValue;

    @Column(nullable = false)
    private BigDecimal impairment = BigDecimal.ZERO;

    @Column(name = "in_service_date")
    private LocalDate inServiceDate;

    @Column(name = "last_verified_date")
    private LocalDate lastVerifiedDate;

    /** Reference of the open change form that is editing this asset, if any. One open form per asset. */
    @Column(name = "locked_by_change")
    private String lockedByChange;

    @Column(name = "disposed_date")
    private LocalDate disposedDate;

    /** Person responsible for the asset day to day. */
    private String custodian;

    @Enumerated(EnumType.STRING)
    private PlantArea area;

    @Enumerated(EnumType.STRING)
    private CapexProgramme programme;

    @Version
    private long version;

    protected Asset() {
    }

    public Asset(String tag, String name, AssetCategory category, AssetStatus status, Site site,
                 String supplier, BigDecimal originalValue, LocalDate inServiceDate) {
        this.tag = tag;
        this.name = name;
        this.category = category;
        this.status = status;
        this.site = site;
        this.supplier = supplier;
        this.originalValue = originalValue;
        this.inServiceDate = inServiceDate;
        this.area = PlantArea.defaultFor(category);
    }

    public void assignArea(PlantArea area) {
        this.area = area;
    }

    public void assignProgramme(CapexProgramme programme) {
        this.programme = programme;
    }

    /** True if the asset was on the books at the end of the given day. Construction has no in-service date yet. */
    public boolean onBooksAt(LocalDate date) {
        boolean started = inServiceDate == null || !inServiceDate.isAfter(date);
        boolean notYetDisposed = disposedDate == null || disposedDate.isAfter(date);
        return started && notYetDisposed;
    }

    public void recordImpairment(BigDecimal amount) {
        this.impairment = this.impairment.add(amount);
    }

    public void markVerified(LocalDate date) {
        this.lastVerifiedDate = date;
    }

    public void lockFor(String changeReference) {
        if (status == AssetStatus.DISPOSED) {
            throw new BusinessRuleException(tag + " is already disposed");
        }
        if (lockedByChange != null) {
            throw new BusinessRuleException(tag + " is already being changed by " + lockedByChange);
        }
        this.lockedByChange = changeReference;
    }

    public void unlock(String changeReference) {
        if (changeReference.equals(lockedByChange)) {
            this.lockedByChange = null;
        }
    }

    public void updateMasterData(String name, String custodian, String supplier) {
        if (name != null && !name.isBlank()) this.name = name;
        if (custodian != null) this.custodian = custodian.isBlank() ? null : custodian;
        if (supplier != null) this.supplier = supplier.isBlank() ? null : supplier;
    }

    public void assignCustodian(String custodian) {
        this.custodian = custodian;
    }

    /** Found during a stocktake but damaged: it needs maintenance before it is back in normal use. */
    public void markDamaged() {
        if (status == AssetStatus.IN_SERVICE) {
            this.status = AssetStatus.UNDER_MAINTENANCE;
        }
    }

    public void moveTo(Site target) {
        this.site = target;
    }

    public void dispose(LocalDate date) {
        this.status = AssetStatus.DISPOSED;
        this.disposedDate = date;
    }

    public Long getId() { return id; }
    public String getTag() { return tag; }
    public String getName() { return name; }
    public AssetCategory getCategory() { return category; }
    public AssetStatus getStatus() { return status; }
    public Site getSite() { return site; }
    public String getSupplier() { return supplier; }
    public BigDecimal getOriginalValue() { return originalValue; }
    public BigDecimal getImpairment() { return impairment; }
    public LocalDate getInServiceDate() { return inServiceDate; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
    public String getLockedByChange() { return lockedByChange; }
    public String getCustodian() { return custodian; }
    public PlantArea getArea() { return area; }
    public CapexProgramme getProgramme() { return programme; }
    public LocalDate getDisposedDate() { return disposedDate; }
}
