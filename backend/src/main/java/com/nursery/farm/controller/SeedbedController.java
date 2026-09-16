package com.nursery.farm.controller;

import com.nursery.farm.entity.Seedbed;
import com.nursery.farm.service.SeedbedService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seedbeds")
public class SeedbedController {

    private final SeedbedService service;

    public SeedbedController(SeedbedService service) {
        this.service = service;
    }

    @GetMapping
    public List<Seedbed> list(@RequestParam(required = false) Long greenhouseId,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword) {
        return service.list(greenhouseId, status, keyword);
    }

    @PostMapping
    public Seedbed create(@RequestBody Seedbed input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    public Seedbed update(@PathVariable Long id, @RequestBody Seedbed input) {
        return service.update(id, input);
    }
}
