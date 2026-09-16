package com.nursery.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 出圃发货单：一批苗发给一个收货方。 */
@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "shipment_no", nullable = false, unique = true, length = 32)
    public String shipmentNo;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    /** 收货方 */
    @Column(nullable = false, length = 64)
    public String customer;

    @Column(nullable = false)
    public Integer qty;

    /** 承运人（司机） */
    @Column(length = 32)
    public String carrier;

    @Column(name = "ship_date")
    public LocalDate shipDate;

    /** 待发货 / 已发货 / 已签收 / 已退回 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
