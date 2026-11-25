package io.github.xijordanwang.assetintel.approval.controller;

import io.github.xijordanwang.assetintel.approval.entity.ApprovalStep;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import io.github.xijordanwang.assetintel.approval.entity.InvestmentRequest;
import io.github.xijordanwang.assetintel.approval.entity.RequestStatus;
import io.github.xijordanwang.assetintel.approval.repository.InvestmentRequestRepository;
import io.github.xijordanwang.assetintel.approval.service.ApprovalService;
import io.github.xijordanwang.assetintel.approval.dto.ApprovalDtos.CreateRequest;
import io.github.xijordanwang.assetintel.approval.dto.ApprovalDtos.DecisionRequest;
import io.github.xijordanwang.assetintel.approval.dto.ApprovalDtos.RequestView;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.security.service.Actor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Investment request workflow. Who acts always comes from the signed-in user, never from the request body:
 * the requester is whoever creates the draft, and a decision is only accepted from the person the waiting
 * step belongs to (see {@link Actor#requireCanDecide}).
 */
@RestController
@RequestMapping("/api/requests")
public class ApprovalController {

    private final ApprovalService service;
    private final InvestmentRequestRepository requests;

    public ApprovalController(ApprovalService service, InvestmentRequestRepository requests) {
        this.service = service;
        this.requests = requests;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RequestView> list(@RequestParam(required = false) RequestStatus status) {
        var found = status == null ? requests.findAllByOrderByCreatedAtDesc()
                : requests.findByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(RequestView::summary).toList();
    }

    @GetMapping("/{reference}")
    @Transactional(readOnly = true)
    public RequestView get(@PathVariable String reference) {
        return RequestView.of(service.get(reference));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RequestView create(@Valid @RequestBody CreateRequest body) {
        return RequestView.of(service.createDraft(body.title(), body.justification(), body.category(),
                body.amount(), body.siteCode(), Actor.current().username()));
    }

    @PostMapping("/{reference}/submit")
    @Transactional
    public RequestView submit(@PathVariable String reference) {
        return RequestView.of(service.submit(reference));
    }

    @PostMapping("/{reference}/approve")
    @Transactional
    public RequestView approve(@PathVariable String reference, @Valid @RequestBody DecisionRequest body) {
        return decide(reference, Decision.APPROVED, body.comment());
    }

    @PostMapping("/{reference}/reject")
    @Transactional
    public RequestView reject(@PathVariable String reference, @Valid @RequestBody DecisionRequest body) {
        return decide(reference, Decision.REJECTED, body.comment());
    }

    private RequestView decide(String reference, Decision decision, String comment) {
        Actor actor = Actor.current();
        InvestmentRequest request = service.get(reference);
        ApproverRole step = request.nextPendingStep().map(ApprovalStep::getApproverRole)
                .orElseThrow(() -> new BusinessRuleException(reference + " is not waiting for a decision"));
        actor.requireCanDecide(step, request.getSite().getPlantCode());
        return RequestView.of(service.decide(reference, step, decision, actor.username(), comment));
    }

    @PostMapping("/{reference}/capitalise")
    @Transactional
    public RequestView capitalise(@PathVariable String reference) {
        return RequestView.of(service.capitalise(reference));
    }
}
