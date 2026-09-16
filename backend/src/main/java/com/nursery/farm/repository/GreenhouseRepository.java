package com.nursery.farm.repository;

import com.nursery.farm.entity.Greenhouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreenhouseRepository extends JpaRepository<Greenhouse, Long> {
    boolean existsByCode(String code);
}
