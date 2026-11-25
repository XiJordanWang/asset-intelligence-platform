package io.github.xijordanwang.assetintel.workspace.controller;

import io.github.xijordanwang.assetintel.workspace.dto.MyWorkView;
import io.github.xijordanwang.assetintel.workspace.service.MyWorkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-work")
public class MyWorkController {

    private final MyWorkService service;

    public MyWorkController(MyWorkService service) {
        this.service = service;
    }

    @GetMapping
    public MyWorkView myWork() {
        return service.forCurrentUser();
    }
}
