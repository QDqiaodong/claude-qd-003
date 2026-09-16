package com.nursery.farm.repository;

import com.nursery.farm.entity.BedOccupancy;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedOccupancyRepository extends JpaRepository<BedOccupancy, Long> {

    List<BedOccupancy> findAllByOrderByFromDateDesc();

    List<BedOccupancy> findBySeedbedIdOrderByFromDateAsc(Long seedbedId);

    /** 这批当前还开放着的占用段（to_date 为空）。一批苗同一时刻只能有一段开放占用。 */
    List<BedOccupancy> findByBatchIdAndToDateIsNull(Long batchId);

    /** 这张床「今天」还被没出圃的批次占着的占用段数（含还没到让出日的段）。改床台账时用。 */
    @Query("select count(o) from BedOccupancy o, NurseryBatch b "
            + "where o.batchId = b.id and o.seedbedId = :seedbedId "
            + "and o.fromDate <= :today and (o.toDate is null or o.toDate > :today) "
            + "and b.status not in :settled")
    long countHoldingOn(@Param("seedbedId") Long seedbedId,
                        @Param("today") LocalDate today,
                        @Param("settled") java.util.Collection<String> settled);

    /**
     * 换棚前核对床位账（FOR UPDATE 锁定读）：这张床上还挂着没出圃批次的「在占」段，即
     *   - 还开放着（to_date 为空）的段：含今天占着的，也含已先排上、还没到播种日的未来档期；
     *   - 或覆盖今天的段：含未来起迁调拨在旧床上留的段（段已切到未来日、但今天苗还没迁走）。
     * 已出圃/已报废批次、以及整段都在过去（to_date <= 今天）的段不拦，那种床已经是空床。
     * 锁定读和开批次写占用段抢同一批行，并发时两笔在这里串行：
     * 后到的换棚会看到先提交那笔新批次开出的段，反之亦然。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BedOccupancy o, NurseryBatch b "
            + "where o.batchId = b.id and o.seedbedId = :seedbedId "
            + "and b.status not in :settled "
            + "and (o.toDate is null or (o.fromDate <= :today and o.toDate > :today))")
    List<BedOccupancy> lockBlockingMoveOn(@Param("seedbedId") Long seedbedId,
                                          @Param("today") LocalDate today,
                                          @Param("settled") java.util.Collection<String> settled);

    /**
     * 开批次撞期校验用：锁定读这张床的全部占用段（FOR UPDATE 锁住这些行）。
     * 床行锁挡住改床台账的写入，这里的锁定读保证拿到的是最新已落账的占用段，
     * 不会因为事务读视图旧而漏掉并发先提交的占用段。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BedOccupancy o where o.seedbedId = :seedbedId "
            + "order by o.fromDate asc")
    List<BedOccupancy> lockBySeedbedOrderByFromDateAsc(@Param("seedbedId") Long seedbedId);
}
