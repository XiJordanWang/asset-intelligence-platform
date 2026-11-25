package io.github.xijordanwang.assetintel.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Lets the React router handle page URLs when the built UI is served by Spring Boot. */
@Controller
public class SpaForwardController {

    @GetMapping({"/cockpit", "/assets", "/requests", "/requests/{reference}", "/changes", "/changes/{reference}",
            "/stocktakes", "/stocktakes/{reference}", "/stocktakes/{reference}/sites/{siteCode}",
            "/plants", "/plants/{plantCode}", "/login", "/admin/users"})
    public String forward() {
        return "forward:/index.html";
    }
}
