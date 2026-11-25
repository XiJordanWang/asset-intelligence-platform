package io.github.xijordanwang.assetintel.workspace.dto;

import java.math.BigDecimal;
import java.util.List;

/** The signed-in person's to-do list, grouped into sections that depend on their roles. */
public record MyWorkView(String username, String displayName, List<Section> sections) {

    /** {@code key} lets the page pick an icon and wording; {@code hint} says what to do with the items. */
    public record Section(String key, String title, String hint, List<Item> items) {
    }

    /** One thing to look at. {@code link} is the page in the app that handles it. */
    public record Item(String reference, String title, String detail, BigDecimal amount, String link) {
    }
}
