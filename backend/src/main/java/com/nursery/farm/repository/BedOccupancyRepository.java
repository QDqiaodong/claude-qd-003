package com.nursery.farm.repository;

import com.nursery.farm.entity.BedOccupancy;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
