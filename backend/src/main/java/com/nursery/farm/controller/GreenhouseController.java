package com.nursery.farm.controller;

import com.nursery.farm.dto.BizException;
import com.nursery.farm.entity.Greenhouse;
import com.nursery.farm.repository.GreenhouseRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/greenhouses")
public class GreenhouseController {

    private final GreenhouseRepository greenhouses;

    public GreenhouseController(GreenhouseRepository greenhouses) {
        this.greenhouses = greenhouses;
    }

    @GetMapping
    public List<Greenhouse> list() {
        return greenhouses.findAll();
    }

    @PostMapping
    public Greenhouse create(@RequestBody Greenhouse input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("温室编号不能为空");
        }
        if (greenhouses.existsByCode(input.code)) {
            throw new BizException("温室编号 " + input.code + " 已经存在");
        }
        Greenhouse saved = new Greenhouse();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "育苗棚" : input.kind;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return greenhouses.save(saved);
    }

    @PutMapping("/{id}/status")
    public Greenhouse setStatus(@PathVariable Long id, @RequestParam String status) {
        Greenhouse house = greenhouses.findById(id)
                .orElseThrow(() -> new BizException("温室不存在"));
        house.status = status;
        return greenhouses.save(house);
    }
}
