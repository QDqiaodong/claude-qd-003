package com.nursery.farm.repository;

import com.nursery.farm.entity.Shipment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    boolean existsByShipmentNo(String shipmentNo);

    List<Shipment> findAllByOrderByUpdatedAtDesc();

    List<Shipment> findByBatchIdOrderByCreatedAtAsc(Long batchId);
}
