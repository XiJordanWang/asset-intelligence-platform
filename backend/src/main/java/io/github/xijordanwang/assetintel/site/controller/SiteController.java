package io.github.xijordanwang.assetintel.site.controller;

import io.github.xijordanwang.assetintel.site.entity.Site;
import io.github.xijordanwang.assetintel.site.repository.SiteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteRepository sites;

    public SiteController(SiteRepository sites) {
        this.sites = sites;
    }

    @GetMapping
    public List<Site> list() {
        return sites.findAll();
    }
}
