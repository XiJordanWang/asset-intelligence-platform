package io.github.xijordanwang.assetintel.stocktake.controller;

import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeCampaign;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeTask;
import io.github.xijordanwang.assetintel.stocktake.repository.StocktakeRepository;
import io.github.xijordanwang.assetintel.stocktake.service.StocktakeService;
import io.github.xijordanwang.assetintel.stocktake.dto.StocktakeDtos.*;
import io.github.xijordanwang.assetintel.security.service.Actor;
import io.github.xijordanwang.assetintel.security.entity.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/stocktakes")
public class StocktakeController {

    private final StocktakeService service;
    private final StocktakeRepository campaigns;

    public StocktakeController(StocktakeService service, StocktakeRepository campaigns) {
        this.service = service;
        this.campaigns = campaigns;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CampaignView> list() {
        return campaigns.findAllByOrderByCreatedAtDesc().stream().map(CampaignView::of).toList();
    }

    @GetMapping("/{reference}")
    @Transactional(readOnly = true)
    public CampaignView get(@PathVariable String reference) {
        return CampaignView.of(service.get(reference));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CampaignView plan(@Valid @RequestBody PlanCampaign body) {
        return CampaignView.of(service.plan(body.title(), body.scopePlant(), body.scopeCategory(),
                body.samplePercent(), body.photoRequired(), body.dueDate(), Actor.current().username()));
    }

    @PostMapping("/{reference}/start")
    @Transactional
    public CampaignView start(@PathVariable String reference) {
        return CampaignView.of(service.start(reference));
    }

    @GetMapping("/{reference}/sites/{siteCode}")
    @Transactional(readOnly = true)
    public SiteTaskView site(@PathVariable String reference, @PathVariable String siteCode) {
        StocktakeCampaign campaign = service.get(reference);
        StocktakeTask task = campaign.task(siteCode);
        return new SiteTaskView(reference, campaign.getStatus(), SiteProgress.of(task),
                task.getLines().stream().map(LineView::of)
                        .sorted(Comparator.comparing(LineView::assetTag)).toList());
    }

    @PostMapping("/{reference}/sites/{siteCode}/count")
    @Transactional
    public SiteProgress count(@PathVariable String reference, @PathVariable String siteCode,
                              @Valid @RequestBody RecordCount body) {
        Actor actor = Actor.current();
        actor.requireCoordinatorFor(siteCode);
        StocktakeCampaign c = service.record(reference, siteCode, body.assetTag(), body.result(), body.note(),
                countingAs(actor, reference, siteCode));
        return SiteProgress.of(c.task(siteCode));
    }

    @PostMapping("/{reference}/sites/{siteCode}/submit")
    @Transactional
    public SiteProgress submit(@PathVariable String reference, @PathVariable String siteCode,
                               @RequestBody(required = false) SubmitSite body) {
        Actor actor = Actor.current();
        actor.requireCoordinatorFor(siteCode);
        return SiteProgress.of(service.submitSite(reference, siteCode, countingAs(actor, reference, siteCode)).task(siteCode));
    }

    @PostMapping("/{reference}/complete")
    @Transactional
    public CompletionView complete(@PathVariable String reference) {
        var result = service.complete(reference);
        return new CompletionView(CampaignView.of(result.campaign()), result.verified(), result.draftDisposals());
    }

    @PostMapping("/{reference}/cancel")
    @Transactional
    public CampaignView cancel(@PathVariable String reference) {
        return CampaignView.of(service.cancel(reference));
    }

    /** Admins may count on behalf of the site's coordinator; everyone else counts as themselves. */
    private String countingAs(Actor actor, String reference, String siteCode) {
        return actor.has(UserRole.ADMIN) ? service.get(reference).task(siteCode).getCoordinator() : actor.username();
    }
}
