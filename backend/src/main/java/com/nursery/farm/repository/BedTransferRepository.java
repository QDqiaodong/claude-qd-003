package com.nursery.farm.repository;

import com.nursery.farm.entity.BedTransfer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedTransferRepository extends JpaRepository<BedTransfer, Long> {

    boolean existsByTransferNo(String transferNo);

    List<BedTransfer> findAllByOrderByCreatedAtDesc();

    List<BedTransfer> findByBatchIdOrderByCreatedAtAsc(Long batchId);
}
