package com.nursery.farm.repository;

import com.nursery.farm.entity.NurseryBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NurseryBatchRepository extends JpaRepository<NurseryBatch, Long> {

    boolean existsByBatchNo(String batchNo);

    List<NurseryBatch> findAllByOrderByUpdatedAtDesc();

    List<NurseryBatch> findBySeedbedIdAndStatusNotIn(Long seedbedId, java.util.Collection<String> statuses);

    long countBySeedbedIdAndStatusNotIn(Long seedbedId, java.util.Collection<String> statuses);
}
