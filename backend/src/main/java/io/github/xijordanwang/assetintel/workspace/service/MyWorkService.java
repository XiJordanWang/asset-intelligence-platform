package io.github.xijordanwang.assetintel.workspace.service;

import io.github.xijordanwang.assetintel.approval.entity.ApprovalStep;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.InvestmentRequest;
import io.github.xijordanwang.assetintel.approval.entity.RequestStatus;
import io.github.xijordanwang.assetintel.approval.repository.InvestmentRequestRepository;
import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.AssetChangeStep;
import io.github.xijordanwang.assetintel.change.entity.ChangeStatus;
import io.github.xijordanwang.assetintel.change.repository.AssetChangeRepository;
import io.github.xijordanwang.assetintel.security.entity.UserRole;
import io.github.xijordanwang.assetintel.security.repository.AppUserRepository;
import io.github.xijordanwang.assetintel.security.service.Actor;
import io.github.xijordanwang.assetintel.stocktake.entity.CampaignStatus;
import io.github.xijordanwang.assetintel.stocktake.entity.StocktakeCampaign;
import io.github.xijordanwang.assetintel.stocktake.entity.TaskStatus;
import io.github.xijordanwang.assetintel.stocktake.repository.StocktakeRepository;
import io.github.xijordanwang.assetintel.workspace.dto.MyWorkView;
import io.github.xijordanwang.assetintel.workspace.dto.MyWorkView.Item;
import io.github.xijordanwang.assetintel.workspace.dto.MyWorkView.Section;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds each person's home page. What appears depends on their roles: approvers see the steps waiting on
 * them, requesters see their drafts and open forms, coordinators see the counts they owe, and finance sees
 * stocktakes to run and approved requests to capitalise.
 */
@Service
@Transactional(readOnly = true)
public class MyWorkService {

    private final InvestmentRequestRepository requests;
    private final AssetChangeRepository changes;
    private final StocktakeRepository stocktakes;
    private final AppUserRepository users;

    public MyWorkService(InvestmentRequestRepository requests, AssetChangeRepository changes,
                         StocktakeRepository stocktakes, AppUserRepository users) {
        this.requests = requests;
        this.changes = changes;
        this.stocktakes = stocktakes;
        this.users = users;
    }

    public MyWorkView forCurrentUser() {
        Actor me = Actor.current();
        List<Section> sections = new ArrayList<>();

        boolean approver = me.has(UserRole.PLANT_MANAGER) || me.has(UserRole.FINANCE_CONTROLLER) || me.has(UserRole.CFO);
        if (approver) {
            List<Item> waiting = new ArrayList<>();
            for (InvestmentRequest r : requests.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING_APPROVAL)) {
                Optional<ApproverRole> step = r.nextPendingStep().map(ApprovalStep::getApproverRole);
                if (step.isPresent() && me.canDecide(step.get(), r.getSite().getPlantCode())) {
                    waiting.add(new Item(r.getReference(), r.getTitle(),
                            "Investment request · " + r.getSite().getCode() + " · by " + r.getRequestedBy(),
                            r.getAmount(), "/requests/" + r.getReference()));
                }
            }
            for (AssetChange c : changes.findByStatusOrderByCreatedAtDesc(ChangeStatus.PENDING_APPROVAL)) {
                Optional<ApproverRole> step = c.nextPendingStep().map(AssetChangeStep::getApproverRole);
                if (step.isEmpty()) continue;
                String plant = step.get() == ApproverRole.RECEIVING_PLANT_MANAGER && c.getTargetSite() != null
                        ? c.getTargetSite().getPlantCode() : c.getAsset().getSite().getPlantCode();
                if (me.canDecide(step.get(), plant)) {
                    waiting.add(new Item(c.getReference(), label(c.getType().name()) + ": " + c.getAsset().getName(),
                            "Asset change · " + c.getAsset().getTag() + " at " + c.getAsset().getSite().getCode()
                                    + " · by " + c.getRequestedBy(),
                            c.getNetBookValue(), "/changes/" + c.getReference()));
                }
            }
            sections.add(new Section("approvals", "Waiting for your approval",
                    "Open each one to approve or reject it.", waiting));
        }

        if (me.has(UserRole.REQUESTER) || me.has(UserRole.PLANT_MANAGER) || me.has(UserRole.ADMIN)) {
            List<Item> mine = new ArrayList<>();
            for (InvestmentRequest r : requests.findAllByOrderByCreatedAtDesc()) {
                if (r.getRequestedBy().equalsIgnoreCase(me.username())
                        && (r.getStatus() == RequestStatus.DRAFT || r.getStatus() == RequestStatus.PENDING_APPROVAL)) {
                    mine.add(new Item(r.getReference(), r.getTitle(), "Investment request · " + label(r.getStatus().name()),
                            r.getAmount(), "/requests/" + r.getReference()));
                }
            }
            for (AssetChange c : changes.findAllByOrderByCreatedAtDesc()) {
                if (c.getRequestedBy().equalsIgnoreCase(me.username()) && c.getStatus().isOpen()) {
                    mine.add(new Item(c.getReference(), label(c.getType().name()) + ": " + c.getAsset().getName(),
                            "Asset change · " + label(c.getStatus().name()), c.getNetBookValue(),
                            "/changes/" + c.getReference()));
                }
            }
            sections.add(new Section("mine", "Your open requests and forms",
                    "Drafts still need submitting; the rest are with approvers.", mine));
        }

        if (me.has(UserRole.SITE_COORDINATOR) || me.has(UserRole.ADMIN)) {
            List<Item> counts = new ArrayList<>();
            for (StocktakeCampaign c : stocktakes.findAllByOrderByCreatedAtDesc()) {
                if (c.getStatus() != CampaignStatus.IN_PROGRESS) continue;
                c.getTasks().stream()
                        .filter(t -> t.getStatus() == TaskStatus.OPEN)
                        .filter(t -> me.has(UserRole.ADMIN)
                                || t.getSite().getPlantCode().equals(me.plantCode()))
                        .forEach(t -> counts.add(new Item(c.getReference() + " · " + t.getSite().getCode(),
                                t.getSite().getName(), c.getTitle() + " · due " + c.getDueDate(), null,
                                "/stocktakes/" + c.getReference() + "/sites/" + t.getSite().getCode())));
            }
            sections.add(new Section("counts", "Sites to count",
                    "Mark each asset found, damaged or missing, then submit the site.", counts));
        }

        if (me.has(UserRole.FINANCE_CONTROLLER) || me.has(UserRole.ADMIN)) {
            List<Item> finance = new ArrayList<>();
            for (InvestmentRequest r : requests.findByStatusOrderByCreatedAtDesc(RequestStatus.APPROVED)) {
                finance.add(new Item(r.getReference(), r.getTitle(), "Approved · ready to capitalise into the register",
                        r.getAmount(), "/requests/" + r.getReference()));
            }
            for (StocktakeCampaign c : stocktakes.findAllByOrderByCreatedAtDesc()) {
                if (c.getStatus() == CampaignStatus.PLANNED) {
                    finance.add(new Item(c.getReference(), c.getTitle(), "Stocktake planned · ready to start", null,
                            "/stocktakes/" + c.getReference()));
                } else if (c.getStatus() == CampaignStatus.IN_PROGRESS) {
                    long open = c.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.OPEN).count();
                    finance.add(new Item(c.getReference(), c.getTitle(), open == 0
                            ? "Stocktake · every site submitted, ready to complete"
                            : "Stocktake in progress · " + open + " of " + c.getTasks().size() + " sites still counting",
                            null, "/stocktakes/" + c.getReference()));
                }
            }
            sections.add(new Section("finance", "Finance follow-ups",
                    "Capitalise approved requests and run stocktakes.", finance));
        }

        String name = users.findByUsername(me.username()).map(u -> u.getDisplayName()).orElse(me.username());
        return new MyWorkView(me.username(), name, sections);
    }

    private static String label(String enumName) {
        String words = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
