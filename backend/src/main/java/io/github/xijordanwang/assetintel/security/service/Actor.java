package io.github.xijordanwang.assetintel.security.service;

import io.github.xijordanwang.assetintel.security.entity.UserRole;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.EnumSet;
import java.util.Set;

/**
 * The signed-in user as the workflows see them. Built from the security context alone: roles are
 * {@code ROLE_*} authorities and the user's plant is a {@code PLANT_*} authority.
 */
public record Actor(String username, Set<UserRole> roles, String plantCode) {

    public static final String PLANT_PREFIX = "PLANT_";

    public static Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Sign in first");
        }
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        String plant = null;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String name = a.getAuthority();
            if (name.startsWith("ROLE_")) {
                try {
                    roles.add(UserRole.valueOf(name.substring(5)));
                } catch (IllegalArgumentException ignored) {
                    // not one of ours
                }
            } else if (name.startsWith(PLANT_PREFIX)) {
                plant = name.substring(PLANT_PREFIX.length());
            }
        }
        return new Actor(auth.getName(), roles, plant);
    }

    public boolean has(UserRole role) {
        return roles.contains(role);
    }

    public void require(UserRole... anyOf) {
        for (UserRole r : anyOf) {
            if (has(r)) return;
        }
        throw new AccessDeniedException(username + " does not have the role for this action");
    }

    /**
     * Whether this user may decide an approval step. Plant-manager steps belong to the manager of the
     * plant concerned: the asset's plant, or for the receiving step, the destination plant.
     */
    public boolean canDecide(ApproverRole step, String plantOfStep) {
        return switch (step) {
            case PLANT_MANAGER, RECEIVING_PLANT_MANAGER -> has(UserRole.PLANT_MANAGER) && plantOfStep != null
                    && plantOfStep.equals(plantCode);
            case FINANCE_CONTROLLER -> has(UserRole.FINANCE_CONTROLLER);
            case CFO -> has(UserRole.CFO);
        };
    }

    public void requireCanDecide(ApproverRole step, String plantOfStep) {
        if (!canDecide(step, plantOfStep)) {
            String who = switch (step) {
                case PLANT_MANAGER -> "the " + plantOfStep + " plant manager";
                case RECEIVING_PLANT_MANAGER -> "the receiving (" + plantOfStep + ") plant manager";
                case FINANCE_CONTROLLER -> "a finance controller";
                case CFO -> "the CFO";
            };
            throw new AccessDeniedException("This step is waiting on " + who + ". Sign in as them to decide it.");
        }
    }

    /** Site coordinators count the sites of their own plant; admins can act for any site. */
    public void requireCoordinatorFor(String siteCode) {
        boolean ownPlant = has(UserRole.SITE_COORDINATOR) && plantCode != null && siteCode.startsWith(plantCode + "-");
        if (!ownPlant && !has(UserRole.ADMIN)) {
            throw new AccessDeniedException("Only the coordinator for " + siteCode + " can record this count");
        }
    }
}
