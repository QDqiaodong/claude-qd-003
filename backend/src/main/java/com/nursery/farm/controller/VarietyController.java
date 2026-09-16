package com.nursery.farm.controller;

import com.nursery.farm.dto.BizException;
import com.nursery.farm.entity.Variety;
import com.nursery.farm.repository.VarietyRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/varieties")
public class VarietyController {

    private final VarietyRepository varieties;

    public VarietyController(VarietyRepository varieties) {
        this.varieties = varieties;
    }

    @GetMapping
    public List<Variety> list() {
        return varieties.findAll();
    }

    @PostMapping
    public Variety create(@RequestBody Variety input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("品种编号不能为空");
        }
        if (varieties.existsByCode(input.code)) {
            throw new BizException("品种编号 " + input.code + " 已经存在");
        }
        Variety saved = new Variety();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.category = (input.category == null || input.category.isBlank()) ? "草本" : input.category;
        saved.status = (input.status == null || input.status.isBlank()) ? "在售" : input.status;
        return varieties.save(saved);
    }

    @PutMapping("/{id}/status")
    public Variety setStatus(@PathVariable Long id, @RequestParam String status) {
        Variety v = varieties.findById(id).orElseThrow(() -> new BizException("品种不存在"));
        v.status = status;
        return varieties.save(v);
    }
}
