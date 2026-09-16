package com.nursery.farm.service;

import com.nursery.farm.dto.BizException;
import com.nursery.farm.entity.Seedbed;
import com.nursery.farm.repository.BedOccupancyRepository;
import com.nursery.farm.repository.GreenhouseRepository;
import com.nursery.farm.repository.SeedbedRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedbedService {

    /** 已经出圃/报废的批次不再占床 */
    private static final List<String> SETTLED = List.of("已出圃", "已报废");

    private final SeedbedRepository seedbeds;
    private final GreenhouseRepository greenhouses;
    private final BedOccupancyRepository occupancies;

    public SeedbedService(SeedbedRepository seedbeds, GreenhouseRepository greenhouses,
                          BedOccupancyRepository occupancies) {
        this.seedbeds = seedbeds;
        this.greenhouses = greenhouses;
        this.occupancies = occupancies;
    }

    public List<Seedbed> list(Long greenhouseId, String status, String keyword) {
        return seedbeds.findAll().stream()
                .filter(s -> greenhouseId == null || greenhouseId.equals(s.greenhouseId))
                .filter(s -> status == null || status.isEmpty() || status.equals(s.status))
                .filter(s -> keyword == null || keyword.isEmpty()
                        || s.name.contains(keyword) || s.code.contains(keyword))
                .toList();
    }

    /** 这张床今天是否还被没出圃的苗占着（看床位账，不看批次现在的床指向）。 */
    private boolean stillHolding(Long seedbedId) {
        return occupancies.countHoldingOn(seedbedId, LocalDate.now(), SETTLED) > 0;
    }

    @Transactional
    public Seedbed create(Seedbed input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("苗床编号不能为空");
        }
        if (seedbeds.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的苗床用掉了");
        }
        if (input.greenhouseId != null && !greenhouses.existsById(input.greenhouseId)) {
            throw new BizException("归属的温室不存在");
        }
        Seedbed saved = new Seedbed();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.greenhouseId = input.greenhouseId;
        saved.capacity = input.capacity == null ? 0 : input.capacity;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return seedbeds.save(saved);
    }

    @Transactional
    public Seedbed update(Long id, Seedbed input) {
        Seedbed bed = seedbeds.findById(id).orElseThrow(() -> new BizException("苗床不存在"));
        if (input.name != null) {
            bed.name = input.name;
        }
        if (input.greenhouseId != null) {
            if (!greenhouses.existsById(input.greenhouseId)) {
                throw new BizException("要挪过去的温室不存在");
            }
            bed.greenhouseId = input.greenhouseId;
        }
        // 实体上 capacity 有默认值 0，这里必须按「>0 才算真的要改」判，否则只改状态也会被当成改容量
        if (input.capacity != null && input.capacity > 0 && !input.capacity.equals(bed.capacity)) {
            if (stillHolding(bed.id)) {
                throw new BizException("这张苗床上还有没出圃的批次，不能改可放株数");
            }
            bed.capacity = input.capacity;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(bed.status)) {
            if (!"在用".equals(input.status) && stillHolding(bed.id)) {
                throw new BizException("苗床 " + bed.name + " 上还有没出圃的批次，先出圃或报废再改成"
                        + input.status);
            }
            bed.status = input.status;
        }
        return seedbeds.save(bed);
    }
}
