package com.nursery.farm.service;

import com.nursery.farm.dto.BizException;
import com.nursery.farm.entity.BedOccupancy;
import com.nursery.farm.entity.BedTransfer;
import com.nursery.farm.entity.Greenhouse;
import com.nursery.farm.entity.NurseryBatch;
import com.nursery.farm.entity.Seedbed;
import com.nursery.farm.repository.BedOccupancyRepository;
import com.nursery.farm.repository.BedTransferRepository;
import com.nursery.farm.repository.GreenhouseRepository;
import com.nursery.farm.repository.NurseryBatchRepository;
import com.nursery.farm.repository.SeedbedRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    /** 还占着苗床的批次状态 */
    private static final List<String> HOLDING = List.of("育苗中", "待出圃");

    private final BedTransferRepository transfers;
    private final BedOccupancyRepository occupancies;
    private final NurseryBatchRepository batches;
    private final SeedbedRepository seedbeds;
    private final GreenhouseRepository greenhouses;

    public TransferService(BedTransferRepository transfers, BedOccupancyRepository occupancies,
                           NurseryBatchRepository batches, SeedbedRepository seedbeds,
                           GreenhouseRepository greenhouses) {
        this.transfers = transfers;
        this.occupancies = occupancies;
        this.batches = batches;
        this.seedbeds = seedbeds;
        this.greenhouses = greenhouses;
    }

    public List<BedTransfer> list(Long batchId) {
        return batchId == null
                ? transfers.findAllByOrderByCreatedAtDesc()
                : transfers.findByBatchIdOrderByCreatedAtAsc(batchId);
    }

    private String nextTransferNo() {
        long n = transfers.count() + 1;
        String no;
        do {
            no = "TR-" + String.format("%04d", n++);
        } while (transfers.existsByTransferNo(no));
        return no;
    }

    /**
     * 开转棚调拨单并直接落账（一张单子一次做完）。
     * 场长的规矩：占用认单不认改名字。单上必须写清从哪张床到哪张床、谁经手、计划哪天迁完。
     *
     * 加锁顺序固定，所有占用写入（开批次、转棚）都先按苗床 id 升序锁床，再锁批次行：
     *   - 同一批被并发调到两张床：两笔抢的是同一批行锁，先到的把批次床位切到甲床，
     *     后到的拿到锁后重读，发现调出床已经对不上 → 失败；
     *   - 对调床（A→B、B→A）：两笔都按 id 升序拿床锁，id 小的那张床先拿，不会互锁。
     * 校验通过才在同一事务里关旧段、开新段、改批次床位；任何一步不通过整笔回滚，
     * 不会出现「新床已经占上、旧床还没放」的半截状态。
     */
    @Transactional
    public BedTransfer post(BedTransfer input) {
        if (input.batchId == null) {
            throw new BizException("请选一个要调拨的批次");
        }
        if (input.fromSeedbedId == null) {
            throw new BizException("请写清从哪张床调出");
        }
        if (input.toSeedbedId == null) {
            throw new BizException("请写清调到哪张床");
        }
        if (input.fromSeedbedId.equals(input.toSeedbedId)) {
            throw new BizException("调出床和调入床是同一张，不用开调拨单");
        }
        if (input.operator == null || input.operator.isBlank()) {
            throw new BizException("请写经手人");
        }
        if (input.planStartDate == null) {
            throw new BizException("请填计划开始迁的日期");
        }
        if (input.planEndDate == null) {
            throw new BizException("请填计划哪天迁完");
        }
        if (input.planEndDate.isBefore(input.planStartDate)) {
            throw new BizException("计划迁完日期不能早于开始迁的日期");
        }
        if (input.planQty == null || input.planQty <= 0) {
            throw new BizException("调拨株数要大于 0");
        }

        NurseryBatch batch = batches.findById(input.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        if (!HOLDING.contains(batch.status)) {
            throw new BizException("批次 " + batch.batchNo + " 现在是 " + batch.status
                    + "，已经不占苗床了，不用再调拨");
        }

        Seedbed fromBed = seedbeds.findById(input.fromSeedbedId)
                .orElseThrow(() -> new BizException("调出床不存在"));
        Seedbed toBed = seedbeds.findById(input.toSeedbedId)
                .orElseThrow(() -> new BizException("调入床不存在"));
        if (!"在用".equals(toBed.status)) {
            throw new BizException("调入床 " + toBed.name + " 现在是" + toBed.status + "，不能接苗");
        }
        if (toBed.greenhouseId == null) {
            throw new BizException("调入床 " + toBed.name + " 还没归到温室，先安排温室");
        }
        Greenhouse house = greenhouses.findById(toBed.greenhouseId)
                .orElseThrow(() -> new BizException("调入床归属的温室已经不存在了，重新安排一下"));
        if (!"在用".equals(house.status)) {
            throw new BizException("调入床所在温室 " + house.name + " 已经停用，里面的苗床不能再接苗");
        }
        if (input.planQty > toBed.capacity) {
            throw new BizException("调入床 " + toBed.name + " 最多放 " + toBed.capacity
                    + " 株，这单要调 " + input.planQty + " 株，放不下");
        }

        // 1) 按 id 升序锁旧床+新床行（全局统一顺序，防止对调床互锁），再锁这批行
        List<Long> bedIds = List.of(fromBed.id, toBed.id).stream().sorted().toList();
        seedbeds.lockByIds(bedIds);
        List<NurseryBatch> locked = batches.lockById(batch.id);
        if (locked.isEmpty()) {
            throw new BizException("批次不存在");
        }
        batch = locked.get(0);
        if (!HOLDING.contains(batch.status)) {
            throw new BizException("批次 " + batch.batchNo + " 现在是 " + batch.status + "，已经不占苗床了");
        }

        LocalDate newFrom = input.planStartDate;
        LocalDate newTo = batch.expectOutDate;

        // 2) 锁内重读这批的开放占用段，确认调出床就是这批现在实际占着的床。
        //    同一批并发往两张床调时，先落账那笔已经把床位切走，后到的在这里对不上 → 失败。
        List<BedOccupancy> openSegs = occupancies.findByBatchIdAndToDateIsNull(batch.id);
        BedOccupancy open = openSegs.isEmpty() ? null : openSegs.get(0);
        if (open == null) {
            throw new BizException("批次 " + batch.batchNo + " 床位账上没有占着的床，没法调");
        }
        if (!open.seedbedId.equals(fromBed.id)) {
            Seedbed nowBed = seedbeds.findById(open.seedbedId).orElse(null);
            throw new BizException("批次 " + batch.batchNo + " 现在占着的是 "
                    + (nowBed == null ? ("#" + open.seedbedId) : nowBed.name)
                    + "，不是 " + fromBed.name + "，调出床对不上（可能已经有一笔调拨先落了账）");
        }
        if (newFrom.isBefore(open.fromDate)) {
            throw new BizException("计划开始迁的日期早于这批上 " + fromBed.name + " 的日期（"
                    + open.fromDate + "），没法往回切");
        }

        // 3) 锁内做新床撞期校验（床行锁挡住了别的占用写入，读到的是最新已落账数据）
        Map<Long, NurseryBatch> batchMap = batches.findAll().stream()
                .collect(Collectors.toMap(b -> b.id, Function.identity()));
        for (BedOccupancy seg : occupancies.findBySeedbedIdOrderByFromDateAsc(toBed.id)) {
            if (seg.batchId.equals(batch.id)) {
                // 自己别的段（多为已关闭的历史段）不跟自己撞
                continue;
            }
            NurseryBatch other = batchMap.get(seg.batchId);
            if (other != null && !HOLDING.contains(other.status)) {
                // 已出圃/已报废的旧段不再占位
                continue;
            }
            if (overlap(newFrom, newTo, seg.fromDate, seg.toDate)) {
                String otherNo = other == null ? ("#" + seg.batchId) : other.batchNo;
                throw new BizException("调入床 " + toBed.name + " 在计划调拨这段日期已经被批次 "
                        + otherNo + " 占着（" + seg.fromDate + " 到 "
                        + (seg.toDate == null ? "未定" : seg.toDate.minusDays(1)) + "），撞期了");
            }
        }

        // 4) 落账：旧床段切到切点（让出切点起的日子），新床段从切点开；批次床位跟着走。
        LocalDateTime now = LocalDateTime.now();
        open.toDate = newFrom;
        open.updatedAt = now;
        occupancies.save(open);

        BedOccupancy fresh = new BedOccupancy();
        fresh.batchId = batch.id;
        fresh.seedbedId = toBed.id;
        fresh.fromDate = newFrom;
        fresh.toDate = newTo;
        fresh.createdAt = now;
        fresh.updatedAt = now;
        occupancies.save(fresh);

        batch.seedbedId = toBed.id;
        batch.updatedAt = now;
        batches.save(batch);

        BedTransfer saved = new BedTransfer();
        saved.transferNo = nextTransferNo();
        saved.batchId = batch.id;
        saved.fromSeedbedId = fromBed.id;
        saved.toSeedbedId = toBed.id;
        saved.planQty = input.planQty;
        saved.operator = input.operator.trim();
        saved.planStartDate = input.planStartDate;
        saved.planEndDate = input.planEndDate;
        saved.status = "已落账";
        saved.createdAt = now;
        saved.updatedAt = now;
        return transfers.save(saved);
    }

    /** 半开区间重叠：[from,to) 与 [oFrom,oTo)，to=null 表示开放到无限远。 */
    static boolean overlap(LocalDate from, LocalDate to, LocalDate oFrom, LocalDate oTo) {
        LocalDate aEnd = to == null ? LocalDate.MAX : to;
        LocalDate bEnd = oTo == null ? LocalDate.MAX : oTo;
        return from.isBefore(bEnd) && oFrom.isBefore(aEnd);
    }
}
