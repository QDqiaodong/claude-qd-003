package com.nursery.farm.repository;

import com.nursery.farm.entity.NurseryBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NurseryBatchRepository extends JpaRepository<NurseryBatch, Long> {

    boolean existsByBatchNo(String batchNo);

    List<NurseryBatch> findAllByOrderByUpdatedAtDesc();

    List<NurseryBatch> findBySeedbedIdAndStatusNotIn(Long seedbedId, java.util.Collection<String> statuses);

    long countBySeedbedIdAndStatusNotIn(Long seedbedId, java.util.Collection<String> statuses);

    /** 锁批次行：同一批并发开到两张床时，两笔在这里串行，先到成功、后到看到的是改完的结果。 */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from NurseryBatch b where b.id = :id")
    List<NurseryBatch> lockById(@Param("id") Long id);
}
