package com.nursery.farm.controller;

import com.nursery.farm.entity.Shipment;
import com.nursery.farm.service.ShipmentService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Shipment> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) Long batchId) {
        return service.list(status, batchId);
    }

    @PostMapping
    public Shipment open(@RequestBody Shipment input) {
        return service.open(input);
    }

    @PostMapping("/{id}/advance")
    public Shipment advance(@PathVariable Long id,
                            @RequestParam String action,
                            @RequestParam(required = false) String carrier,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shipDate) {
        return service.advance(id, action, carrier, shipDate);
    }
}
