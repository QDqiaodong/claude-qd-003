package com.nursery.farm.controller;

import com.nursery.farm.entity.NurseryBatch;
import com.nursery.farm.service.BatchService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @GetMapping
    public List<NurseryBatch> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long seedbedId,
                                   @RequestParam(required = false) Long varietyId,
                                   @RequestParam(required = false) String keyword) {
        return service.list(status, seedbedId, varietyId, keyword);
    }

    @PostMapping
    public NurseryBatch open(@RequestBody NurseryBatch input) {
        return service.open(input);
    }

    @PostMapping("/{id}/advance")
    public NurseryBatch advance(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) Integer actualQty) {
        return service.advance(id, action, actualQty);
    }
}
