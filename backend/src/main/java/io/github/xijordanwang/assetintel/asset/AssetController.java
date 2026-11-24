package io.github.xijordanwang.assetintel.asset;

import io.github.xijordanwang.assetintel.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/assets")
@Transactional(readOnly = true)
public class AssetController {

    private final AssetRepository assets;
    private final Clock clock;

    public AssetController(AssetRepository assets, Clock clock) {
        this.assets = assets;
        this.clock = clock;
    }

    @GetMapping
    public Page<AssetView> search(@RequestParam(required = false) String site,
                                  @RequestParam(required = false) AssetCategory category,
                                  @RequestParam(required = false) AssetStatus status,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        LocalDate date = asOf != null ? asOf : LocalDate.now(clock);
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("tag"));
        Page<Asset> result = assets.search(site, category, status, pageable);
        return result.map(a -> AssetView.of(a, date));
    }

    @GetMapping("/{tag}")
    public AssetView get(@PathVariable String tag,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now(clock);
        return assets.findByTag(tag)
                .map(a -> AssetView.of(a, date))
                .orElseThrow(() -> new NotFoundException("No asset with tag " + tag));
    }
}
