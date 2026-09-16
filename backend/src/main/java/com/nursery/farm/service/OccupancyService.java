package com.nursery.farm.service;

import com.nursery.farm.dto.OccupancyRow;
import com.nursery.farm.entity.BedOccupancy;
import com.nursery.farm.entity.Greenhouse;
import com.nursery.farm.entity.NurseryBatch;
import com.nursery.farm.entity.Seedbed;
import com.nursery.farm.entity.Variety;
import com.nursery.farm.repository.BedOccupancyRepository;
import com.nursery.farm.repository.GreenhouseRepository;
import com.nursery.farm.repository.NurseryBatchRepository;
import com.nursery.farm.repository.SeedbedRepository;
import com.nursery.farm.repository.VarietyRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OccupancyService {

    private final BedOccupancyRepository occupancies;
    private final NurseryBatchRepository batches;
    private final SeedbedRepository seedbeds;
    private final GreenhouseRepository greenhouses;
    private final VarietyRepository varieties;

    public OccupancyService(BedOccupancyRepository occupancies, NurseryBatchRepository batches,
                            SeedbedRepository seedbeds, GreenhouseRepository greenhouses,
                            VarietyRepository varieties) {
        this.occupancies = occupancies;
        this.batches = batches;
        this.seedbeds = seedbeds;
        this.greenhouses = greenhouses;
        this.varieties = varieties;
    }

    /** 床位账全量分段（占用段），按开始日期倒序。 */
    @Transactional(readOnly = true)
    public List<OccupancyRow> listRows(Long seedbedId, Long greenhouseId, String batchNo) {
        Map<Long, NurseryBatch> batchMap = batches.findAll().stream()
                .collect(Collectors.toMap(b -> b.id, Function.identity()));
        Map<Long, Seedbed> bedMap = seedbeds.findAll().stream()
                .collect(Collectors.toMap(b -> b.id, Function.identity()));
        Map<Long, Greenhouse> houseMap = greenhouses.findAll().stream()
                .collect(Collectors.toMap(h -> h.id, Function.identity()));
        Map<Long, Variety> varietyMap = varieties.findAll().stream()
                .collect(Collectors.toMap(v -> v.id, Function.identity()));

        return occupancies.findAllByOrderByFromDateDesc().stream()
                .filter(o -> seedbedId == null || seedbedId.equals(o.seedbedId))
                .filter(o -> {
                    if (greenhouseId == null) {
                        return true;
                    }
                    Seedbed bed = bedMap.get(o.seedbedId);
                    return bed != null && greenhouseId.equals(bed.greenhouseId);
                })
                .filter(o -> {
                    if (batchNo == null || batchNo.isBlank()) {
                        return true;
                    }
                    NurseryBatch b = batchMap.get(o.batchId);
                    return b != null && b.batchNo.contains(batchNo);
                })
                .map(o -> {
                    NurseryBatch b = batchMap.get(o.batchId);
                    Seedbed bed = bedMap.get(o.seedbedId);
                    Greenhouse house = bed == null ? null : houseMap.get(bed.greenhouseId);
                    Variety variety = b == null ? null : varietyMap.get(b.varietyId);
                    return toRow(o,
                            Optional.ofNullable(b),
                            Optional.ofNullable(bed),
                            Optional.ofNullable(house),
                            variety);
                })
                .toList();
    }

    private OccupancyRow toRow(BedOccupancy o, Optional<NurseryBatch> batch,
                               Optional<Seedbed> bed, Optional<Greenhouse> house,
                               Variety variety) {
        OccupancyRow row = new OccupancyRow();
        row.id = o.id;
        row.batchId = o.batchId;
        row.fromDate = o.fromDate;
        row.toDate = o.toDate;
        row.open = o.toDate == null;
        batch.ifPresent(b -> {
            row.batchNo = b.batchNo;
            row.planQty = b.planQty;
            row.batchStatus = b.status;
            row.grower = b.grower;
            row.varietyId = b.varietyId;
        });
        bed.ifPresent(s -> {
            row.seedbedId = s.id;
            row.seedbedCode = s.code;
            row.seedbedName = s.name;
            row.seedbedStatus = s.status;
            row.capacity = s.capacity;
            row.greenhouseId = s.greenhouseId;
        });
        house.ifPresent(h -> row.greenhouseName = h.name);
        if (variety != null) {
            row.varietyName = variety.name;
        }
        return row;
    }
}
