package io.github.xijordanwang.assetintel.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Lets the React router handle page URLs when the built UI is served by Spring Boot. */
@Controller
public class SpaForwardController {

    @GetMapping({"/cockpit", "/assets", "/requests", "/requests/{reference}"})
    public String forward() {
        return "forward:/index.html";
    }
}
