package io.github.xijordanwang.assetintel.approval;

import io.github.xijordanwang.assetintel.approval.ApprovalDtos.CreateRequest;
import io.github.xijordanwang.assetintel.approval.ApprovalDtos.DecisionRequest;
import io.github.xijordanwang.assetintel.approval.ApprovalDtos.RequestView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Investment request workflow. In this early version the approver role is passed in the body;
 * it will come from the signed-in demo user once Spring Security is added.
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
                body.amount(), body.siteCode(), body.requestedBy()));
    }

    @PostMapping("/{reference}/submit")
    @Transactional
    public RequestView submit(@PathVariable String reference) {
        return RequestView.of(service.submit(reference));
    }

    @PostMapping("/{reference}/approve")
    @Transactional
    public RequestView approve(@PathVariable String reference, @Valid @RequestBody DecisionRequest body) {
        return RequestView.of(service.decide(reference, body.role(), Decision.APPROVED, body.decidedBy(), body.comment()));
    }

    @PostMapping("/{reference}/reject")
    @Transactional
    public RequestView reject(@PathVariable String reference, @Valid @RequestBody DecisionRequest body) {
        return RequestView.of(service.decide(reference, body.role(), Decision.REJECTED, body.decidedBy(), body.comment()));
    }

    @PostMapping("/{reference}/capitalise")
    @Transactional
    public RequestView capitalise(@PathVariable String reference) {
        return RequestView.of(service.capitalise(reference));
    }
}
