package com.nursery.farm.repository;

import com.nursery.farm.entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VarietyRepository extends JpaRepository<Variety, Long> {
    boolean existsByCode(String code);
}
