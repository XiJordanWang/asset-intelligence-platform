package io.github.xijordanwang.assetintel.change.controller;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.AssetChangeStep;
import io.github.xijordanwang.assetintel.change.entity.ChangeStatus;
import io.github.xijordanwang.assetintel.change.repository.AssetChangeRepository;
import io.github.xijordanwang.assetintel.change.service.AssetChangeService;
import io.github.xijordanwang.assetintel.approval.dto.ApprovalDtos.DecisionRequest;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import io.github.xijordanwang.assetintel.change.dto.AssetChangeDtos.CancelChange;
import io.github.xijordanwang.assetintel.change.dto.AssetChangeDtos.ChangeView;
import io.github.xijordanwang.assetintel.change.dto.AssetChangeDtos.CreateChange;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.security.service.Actor;
import io.github.xijordanwang.assetintel.security.entity.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/changes")
public class AssetChangeController {

    private final AssetChangeService service;
    private final AssetChangeRepository changes;

    public AssetChangeController(AssetChangeService service, AssetChangeRepository changes) {
        this.service = service;
        this.changes = changes;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ChangeView> list(@RequestParam(required = false) ChangeStatus status) {
        var found = status == null ? changes.findAllByOrderByCreatedAtDesc() : changes.findByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(c -> ChangeView.of(c, false)).toList();
    }

    @GetMapping("/{reference}")
    @Transactional(readOnly = true)
    public ChangeView get(@PathVariable String reference) {
        return ChangeView.of(service.get(reference), true);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ChangeView create(@Valid @RequestBody CreateChange body) {
        return ChangeView.of(service.createDraft(body.type(), body.assetTag(), Actor.current().username(), body.reason(),
                body.targetSiteCode(), body.disposalReason(), body.newName(), body.newCustodian(), body.newSupplier()), true);
    }

    @PostMapping("/{reference}/submit")
    @Transactional
    public ChangeView submit(@PathVariable String reference) {
        return ChangeView.of(service.submit(reference), true);
    }

    @PostMapping("/{reference}/approve")
    @Transactional
    public ChangeView approve(@PathVariable String reference, @Valid @RequestBody DecisionRequest body) {
        return decide(reference, Decision.APPROVED, body.comment());
    }

    @PostMapping("/{reference}/reject")
    @Transactional
    public ChangeView reject(@PathVariable String reference, @Valid @RequestBody DecisionRequest body) {
        return decide(reference, Decision.REJECTED, body.comment());
    }

    @PostMapping("/{reference}/cancel")
    @Transactional
    public ChangeView cancel(@PathVariable String reference, @RequestBody(required = false) CancelChange body) {
        Actor actor = Actor.current();
        AssetChange change = service.get(reference);
        // An admin may cancel on the requester's behalf; anyone else must be the requester.
        String by = actor.has(UserRole.ADMIN) ? change.getRequestedBy() : actor.username();
        return ChangeView.of(service.cancel(reference, by), true);
    }

    private ChangeView decide(String reference, Decision decision, String comment) {
        Actor actor = Actor.current();
        AssetChange change = service.get(reference);
        ApproverRole step = change.nextPendingStep().map(AssetChangeStep::getApproverRole)
                .orElseThrow(() -> new BusinessRuleException(reference + " is not waiting for a decision"));
        String plant = step == ApproverRole.RECEIVING_PLANT_MANAGER && change.getTargetSite() != null
                ? change.getTargetSite().getPlantCode()
                : change.getAsset().getSite().getPlantCode();
        actor.requireCanDecide(step, plant);
        return ChangeView.of(service.decide(reference, step, decision, actor.username(), comment), true);
    }
}
