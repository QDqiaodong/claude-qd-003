package com.nursery.farm.controller;

import com.nursery.farm.dto.OccupancyRow;
import com.nursery.farm.service.OccupancyService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/occupancies")
public class OccupancyController {

    private final OccupancyService service;

    public OccupancyController(OccupancyService service) {
        this.service = service;
    }

    @GetMapping
    public List<OccupancyRow> list(@RequestParam(required = false) Long seedbedId,
                                   @RequestParam(required = false) Long greenhouseId,
                                   @RequestParam(required = false) String batchNo) {
        return service.listRows(seedbedId, greenhouseId, batchNo);
    }
}
