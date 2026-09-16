package com.nursery.farm.service;

import com.nursery.farm.dto.BizException;
import com.nursery.farm.entity.BedOccupancy;
import com.nursery.farm.entity.Seedbed;
import com.nursery.farm.repository.BedOccupancyRepository;
import com.nursery.farm.repository.GreenhouseRepository;
import com.nursery.farm.repository.NurseryBatchRepository;
import com.nursery.farm.repository.SeedbedRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedbedService {

    /** 已经出圃/报废的批次不再占床 */
    private static final List<String> SETTLED = List.of("已出圃", "已报废");

    private final SeedbedRepository seedbeds;
    private final GreenhouseRepository greenhouses;
    private final BedOccupancyRepository occupancies;
    private final NurseryBatchRepository batches;

    public SeedbedService(SeedbedRepository seedbeds, GreenhouseRepository greenhouses,
                          BedOccupancyRepository occupancies, NurseryBatchRepository batches) {
        this.seedbeds = seedbeds;
        this.greenhouses = greenhouses;
        this.occupancies = occupancies;
        this.batches = batches;
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

    /**
     * 换棚核对（锁定读）：这张床上还挂着没出圃批次的在占段——开放段（含已排上的未来档期）
     * 或覆盖今天的段（含未来起迁调拨还没迁走的旧床段）。空床（段都在过去、批次已了结）返回空。
     */
    private List<BedOccupancy> blockingMove(Long seedbedId) {
        return occupancies.lockBlockingMoveOn(seedbedId, LocalDate.now(), SETTLED);
    }

    private String blockingBatchNos(List<BedOccupancy> segs) {
        return segs.stream()
                .map(s -> batches.findById(s.batchId).map(b -> b.batchNo).orElse("#" + s.batchId))
                .distinct()
                .collect(Collectors.joining("、"));
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

    /**
     * 调整苗床台账。
     *
     * 改归属（换棚）必须和床位账同一笔核对：先和开批次/转棚一样锁住床行，
     * 再锁定读床位账上这张床还有没有没出圃批次的在占段——有就整笔拒绝，
     * 只有已经空出来的床才能换棚。床行锁（加占用行锁）同时把「换棚」和
     * 「往这张床开新批次」串成先后两笔：
     * 先到的换了棚，后到的开批次在锁后重读会看到新归属/新状态；
     * 先到的开了批次，后到的换棚在这里读到新占用段而失败。
     * 任何一步不通过都抛错回滚，不会留下「床已挂新棚、日期段还写旧棚」的半截账。
     */
    @Transactional
    public Seedbed update(Long id, Seedbed input) {
        // 1) 先锁床行，后面所有核对都看锁定后读到的最新状态（不拿锁前的旧快照做判断）
        List<Seedbed> locked = seedbeds.lockByIds(List.of(id));
        Seedbed bed = locked.isEmpty() ? null : locked.get(0);
        if (bed == null) {
            throw new BizException("苗床不存在");
        }

        boolean movingHouse = input.greenhouseId != null
                && !input.greenhouseId.equals(bed.greenhouseId);
        boolean changingStatus = input.status != null && !input.status.isBlank()
                && !input.status.equals(bed.status);
        // 实体上 capacity 有默认值 0，这里必须按「>0 才算真的要改」判，否则只改状态也会被当成改容量
        boolean changingCapacity = input.capacity != null && input.capacity > 0
                && !input.capacity.equals(bed.capacity);

        // 2) 换棚：先核目标棚，再锁内核对床位账——还挂着没出圃的在占段就不许换。
        //    锁定读和开批次写占用段互斥，并发两笔只有一笔能过；失败前面就抛错，
        //    归属尚未改动，整笔回滚，不会留下「床已挂新棚、日期段还写旧棚」的半截账。
        if (movingHouse) {
            if (!greenhouses.existsById(input.greenhouseId)) {
                throw new BizException("要挪过去的温室不存在");
            }
            List<BedOccupancy> blocking = blockingMove(bed.id);
            if (!blocking.isEmpty()) {
                throw new BizException("苗床 " + bed.name + " 上还有没出圃的批次 "
                        + blockingBatchNos(blocking)
                        + "，先把苗出圃或报废、腾空床位再换棚（床位账上的日期段还在这张床上）");
            }
        }

        // 3) 改可放株数 / 改成非在用状态：老规矩，今天还被没出圃的苗占着的床不许动。
        //    床行锁已把并发开批次挡在后面，这里读到的是最新已落账结果，不会两边都过。
        if ((changingCapacity || (changingStatus && !"在用".equals(input.status)))
                && stillHolding(bed.id)) {
            if (changingCapacity) {
                throw new BizException("这张苗床上还有没出圃的批次，不能改可放株数");
            }
            throw new BizException("苗床 " + bed.name + " 上还有没出圃的批次，先出圃或报废再改成"
                    + input.status);
        }

        // 4) 核对全过才落账：归属、容量、状态在同一事务同一把锁下改，
        //    失败前面就已抛错回滚，不会出现归属改了一半的状态。
        if (input.name != null) {
            bed.name = input.name;
        }
        if (movingHouse) {
            bed.greenhouseId = input.greenhouseId;
        }
        if (changingCapacity) {
            bed.capacity = input.capacity;
        }
        if (changingStatus) {
            bed.status = input.status;
        }
        return seedbeds.save(bed);
    }
}
