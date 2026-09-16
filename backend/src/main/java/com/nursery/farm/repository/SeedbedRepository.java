package com.nursery.farm.repository;

import com.nursery.farm.entity.Seedbed;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeedbedRepository extends JpaRepository<Seedbed, Long> {
    boolean existsByCode(String code);

    /** 按 id 升序锁一批苗床行。所有占用写入（开批次/转棚）都先按同一顺序锁床，避免互锁死锁。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seedbed s where s.id in :ids order by s.id asc")
    List<Seedbed> lockByIds(@Param("ids") java.util.Collection<Long> ids);
}
