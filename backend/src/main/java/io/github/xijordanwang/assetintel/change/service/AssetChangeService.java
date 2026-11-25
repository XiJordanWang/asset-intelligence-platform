package io.github.xijordanwang.assetintel.change.service;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.change.entity.DisposalReason;
import io.github.xijordanwang.assetintel.change.repository.AssetChangeRepository;
import io.github.xijordanwang.assetintel.change.service.handler.ChangeHandler;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.asset.entity.AssetStatus;
import io.github.xijordanwang.assetintel.asset.service.DepreciationCalculator;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.common.exception.NotFoundException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import io.github.xijordanwang.assetintel.site.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One generic workflow for every change type; the per-type rules come from the handlers. */
@Service
@Transactional
public class AssetChangeService {

    private static final ZoneId NZ = ZoneId.of("Pacific/Auckland");

    private final AssetChangeRepository changes;
    private final AssetRepository assets;
    private final SiteRepository sites;
    private final Map<ChangeType, ChangeHandler> handlers = new EnumMap<>(ChangeType.class);
    private final Clock clock;

    public AssetChangeService(AssetChangeRepository changes, AssetRepository assets, SiteRepository sites,
                              List<ChangeHandler> handlerBeans, Clock clock) {
        this.changes = changes;
        this.assets = assets;
        this.sites = sites;
        this.clock = clock;
        handlerBeans.forEach(h -> handlers.put(h.type(), h));
    }

    public AssetChange createDraft(ChangeType type, String assetTag, String requestedBy, String reason,
                                   String targetSiteCode, DisposalReason disposalReason) {
        return createDraft(type, assetTag, requestedBy, reason, targetSiteCode, disposalReason, null, null, null);
    }

    public AssetChange createDraft(ChangeType type, String assetTag, String requestedBy, String reason,
                                   String targetSiteCode, DisposalReason disposalReason,
                                   String newName, String newCustodian, String newSupplier) {
        Asset asset = assets.findByTag(assetTag)
                .orElseThrow(() -> new NotFoundException("No asset with tag " + assetTag));
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new BusinessRuleException(assetTag + " is already disposed");
        }
        Site target = targetSiteCode == null || targetSiteCode.isBlank() ? null
                : sites.findByCode(targetSiteCode)
                        .orElseThrow(() -> new NotFoundException("No site with code " + targetSiteCode));
        String prefix = switch (type) {
            case TRANSFER -> "TR";
            case DISPOSAL -> "DS";
            case MODIFICATION -> "MD";
        };
        String reference = "%s-%d-%05d".formatted(prefix, LocalDate.now(clock.withZone(NZ)).getYear(),
                changes.countByType(type) + 1);
        AssetChange change = new AssetChange(reference, type, asset, requestedBy, reason, target,
                type == ChangeType.DISPOSAL ? disposalReason : null, clock.instant());
        if (type == ChangeType.MODIFICATION) {
            change.propose(newName, newCustodian, newSupplier);
        }
        handler(type).validate(change);
        return changes.save(change);
    }

    /** Captures the net book value, works out the route and locks the asset against other changes. */
    public AssetChange submit(String reference) {
        AssetChange change = get(reference);
        var nbv = DepreciationCalculator.netBookValue(change.getAsset(), LocalDate.now(clock.withZone(NZ)));
        change.submit(handler(change.getType()).route(change, nbv), nbv, clock.instant());
        return change;
    }

    public AssetChange decide(String reference, ApproverRole role, Decision decision, String decidedBy, String comment) {
        AssetChange change = get(reference);
        boolean approved = change.decide(role, decision, decidedBy, comment, clock.instant());
        if (approved) {
            handler(change.getType()).apply(change, LocalDate.now(clock.withZone(NZ)));
        }
        return change;
    }

    public AssetChange cancel(String reference, String by) {
        AssetChange change = get(reference);
        change.cancel(by, clock.instant());
        return change;
    }

    @Transactional(readOnly = true)
    public AssetChange get(String reference) {
        return changes.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("No asset change " + reference));
    }

    private ChangeHandler handler(ChangeType type) {
        ChangeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + type);
        }
        return handler;
    }
}
