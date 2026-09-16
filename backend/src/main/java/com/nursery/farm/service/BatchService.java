package com.nursery.farm.service;

import com.nursery.farm.dto.BizException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchService {

    /** 已经了结、不再占床的状态 */
    private static final List<String> SETTLED = List.of("已出圃", "已报废");

    private final NurseryBatchRepository batches;
    private final VarietyRepository varieties;
    private final SeedbedRepository seedbeds;
    private final GreenhouseRepository greenhouses;
    private final BedOccupancyRepository occupancies;

    public BatchService(NurseryBatchRepository batches, VarietyRepository varieties,
                        SeedbedRepository seedbeds, GreenhouseRepository greenhouses,
                        BedOccupancyRepository occupancies) {
        this.batches = batches;
        this.varieties = varieties;
        this.seedbeds = seedbeds;
        this.greenhouses = greenhouses;
        this.occupancies = occupancies;
    }

    public List<NurseryBatch> list(String status, Long seedbedId, Long varietyId, String keyword) {
        return batches.findAllByOrderByUpdatedAtDesc().stream()
                .filter(b -> status == null || status.isEmpty() || status.equals(b.status))
                .filter(b -> seedbedId == null || seedbedId.equals(b.seedbedId))
                .filter(b -> varietyId == null || varietyId.equals(b.varietyId))
                .filter(b -> keyword == null || keyword.isEmpty()
                        || b.batchNo.contains(keyword)
                        || (b.grower != null && b.grower.contains(keyword)))
                .toList();
    }

    private String nextBatchNo() {
        long n = batches.count() + 1;
        String no;
        do {
            no = "NB-" + String.format("%04d", n++);
        } while (batches.existsByBatchNo(no));
        return no;
    }

    @Transactional
    public NurseryBatch open(NurseryBatch input) {
        if (input.varietyId == null) {
            throw new BizException("请选一个品种");
        }
        if (input.seedbedId == null) {
            throw new BizException("请选一张苗床");
        }
        if (input.sowDate == null) {
            throw new BizException("请填播种日期");
        }
        if (input.planQty == null || input.planQty <= 0) {
            throw new BizException("计划株数要大于 0");
        }
        if (input.expectOutDate != null && input.expectOutDate.isBefore(input.sowDate)) {
            throw new BizException("预计出圃日期不能早于播种日期");
        }

        Variety variety = varieties.findById(input.varietyId)
                .orElseThrow(() -> new BizException("品种不存在"));
        if (!"在售".equals(variety.status)) {
            throw new BizException("品种 " + variety.name + " 已经停用，不能再开新批次");
        }
        Seedbed bed = seedbeds.findById(input.seedbedId)
                .orElseThrow(() -> new BizException("苗床不存在"));
        if (!"在用".equals(bed.status)) {
            throw new BizException("苗床 " + bed.name + " 现在是" + bed.status + "，不能育新苗");
        }
        if (bed.greenhouseId == null) {
            throw new BizException("苗床 " + bed.name + " 还没归到温室，先安排温室");
        }
        Greenhouse house = greenhouses.findById(bed.greenhouseId)
                .orElseThrow(() -> new BizException("苗床归属的温室已经不存在了，重新安排一下"));
        if (!"在用".equals(house.status)) {
            throw new BizException("温室 " + house.name + " 已经停用，里面的苗床不能再育新苗");
        }
        if (input.planQty > bed.capacity) {
            throw new BizException("苗床 " + bed.name + " 最多放 " + bed.capacity
                    + " 株，这批要下 " + input.planQty + " 株，放不下");
        }

        LocalDate from = input.sowDate;
        LocalDate to = input.expectOutDate;
        // 先锁床行（和转棚调拨同一把锁、同一个顺序），再读占用段做撞期校验，并发也不会撞
        seedbeds.lockByIds(List.of(bed.id));
        for (BedOccupancy seg : occupancies.findBySeedbedIdOrderByFromDateAsc(bed.id)) {
            NurseryBatch other = batches.findById(seg.batchId).orElse(null);
            if (other != null && SETTLED.contains(other.status)) {
                continue;
            }
            if (TransferService.overlap(from, to, seg.fromDate, seg.toDate)) {
                String otherNo = other == null ? ("#" + seg.batchId) : other.batchNo;
                throw new BizException("苗床 " + bed.name + " 在这个时间段已经被批次 "
                        + otherNo + " 占着（" + seg.fromDate + " 到 "
                        + (seg.toDate == null ? "未定" : seg.toDate.minusDays(1)) + "）");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        NurseryBatch saved = new NurseryBatch();
        saved.batchNo = nextBatchNo();
        saved.varietyId = variety.id;
        saved.seedbedId = bed.id;
        saved.sowDate = from;
        saved.expectOutDate = input.expectOutDate;
        saved.planQty = input.planQty;
        saved.actualQty = 0;
        saved.grower = input.grower;
        saved.status = "育苗中";
        saved.createdAt = now;
        saved.updatedAt = now;
        saved = batches.save(saved);

        // 床位账：开一批就开一段开放占用（有预计出圃就写到那天，没有就先开放）
        BedOccupancy occ = new BedOccupancy();
        occ.batchId = saved.id;
        occ.seedbedId = bed.id;
        occ.fromDate = from;
        occ.toDate = to;
        occ.createdAt = now;
        occ.updatedAt = now;
        occupancies.save(occ);

        return saved;
    }

    /** 育苗中 -> 待出圃 -> 已出圃；育苗中/待出圃 都可以报废。 */
    @Transactional
    public NurseryBatch advance(Long id, String action, Integer actualQty) {
        NurseryBatch batch = batches.findById(id).orElseThrow(() -> new BizException("批次不存在"));

        if ("ready".equals(action)) {
            if (!"育苗中".equals(batch.status)) {
                throw new BizException("只有育苗中的批次能转待出圃，这批现在是 " + batch.status);
            }
            if (actualQty == null || actualQty <= 0) {
                throw new BizException("转待出圃要填实际成苗株数");
            }
            batch.actualQty = actualQty;
            batch.status = "待出圃";
        } else if ("out".equals(action)) {
            if (!"待出圃".equals(batch.status)) {
                throw new BizException("只有待出圃的批次能出圃，这批现在是 " + batch.status);
            }
            batch.status = "已出圃";
        } else if ("scrap".equals(action)) {
            if ("已出圃".equals(batch.status)) {
                throw new BizException("已经出圃的批次不能报废");
            }
            if ("已报废".equals(batch.status)) {
                throw new BizException("这批已经报废过了");
            }
            batch.status = "已报废";
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        batch.updatedAt = LocalDateTime.now();
        return batches.save(batch);
    }
}
