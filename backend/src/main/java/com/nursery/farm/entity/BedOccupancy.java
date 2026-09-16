package com.nursery.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 苗床占用段（床位账）：一批苗在某张苗床上占的一段日期。
 * 一张床一段时间只能被一批苗占着；to_date 为空表示一直占着（开放区间）。
 * 转棚调拨会把一段切成两段：旧床段在调拨切点关闭，新床段从切点开始。
 */
@Entity
@Table(name = "bed_occupancy")
public class BedOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    @Column(name = "seedbed_id", nullable = false)
    public Long seedbedId;

    /** 占用开始日期（含） */
    @Column(name = "from_date", nullable = false)
    public LocalDate fromDate;

    /** 占用结束日期（不含）；null 表示还占着，没有截止 */
    @Column(name = "to_date")
    public LocalDate toDate;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
