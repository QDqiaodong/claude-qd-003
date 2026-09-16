package com.nursery.farm.controller;

import com.nursery.farm.entity.BedTransfer;
import com.nursery.farm.service.TransferService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @GetMapping
    public List<BedTransfer> list(@RequestParam(required = false) Long batchId) {
        return service.list(batchId);
    }

    /** 开转棚调拨单并直接落账：旧床让出、新床占用，一次事务做完。 */
    @PostMapping
    public BedTransfer post(@RequestBody BedTransfer input) {
        return service.post(input);
    }
}
