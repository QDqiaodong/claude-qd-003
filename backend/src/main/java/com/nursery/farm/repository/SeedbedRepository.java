package com.nursery.farm.repository;

import com.nursery.farm.entity.Seedbed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedbedRepository extends JpaRepository<Seedbed, Long> {
    boolean existsByCode(String code);
}
