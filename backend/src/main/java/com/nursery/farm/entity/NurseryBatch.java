package com.nursery.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 育苗批次：一批苗从播种到出圃。占用一张苗床的一段日期。 */
@Entity
@Table(name = "nursery_batch")
public class NurseryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "batch_no", nullable = false, unique = true, length = 32)
    public String batchNo;

    @Column(name = "variety_id", nullable = false)
    public Long varietyId;

    @Column(name = "seedbed_id")
    public Long seedbedId;

    @Column(name = "sow_date", nullable = false)
    public LocalDate sowDate;

    @Column(name = "expect_out_date")
    public LocalDate expectOutDate;

    /** 计划株数，不能超过苗床可放株数 */
    @Column(name = "plan_qty", nullable = false)
    public Integer planQty;

    /** 实际成苗株数，出圃前为 0 */
    @Column(name = "actual_qty", nullable = false)
    public Integer actualQty;

    /** 负责育苗的师傅 */
    @Column(length = 32)
    public String grower;

    /** 育苗中 / 待出圃 / 已出圃 / 已报废 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
