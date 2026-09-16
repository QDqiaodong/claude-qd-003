package com.nursery.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转棚调拨单（批次账）：一批苗从旧床调到新床的凭据。
 * 场长的规矩：只改批次上的棚名字、床位不跟着走不算数；
 * 必须落一张调拨单，写清从哪张床到哪张床、谁经手、计划哪天迁完，占用才认转移。
 * 调拨单一落账即生效，所以单子只有「已落账」一个状态。
 */
@Entity
@Table(name = "bed_transfer")
public class BedTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "transfer_no", nullable = false, unique = true, length = 32)
    public String transferNo;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    @Column(name = "from_seedbed_id", nullable = false)
    public Long fromSeedbedId;

    @Column(name = "to_seedbed_id", nullable = false)
    public Long toSeedbedId;

    /** 调拨株数，不能超过新床可放株数 */
    @Column(name = "plan_qty", nullable = false)
    public Integer planQty;

    /** 经手人 */
    @Column(nullable = false, length = 32)
    public String operator;

    /** 计划开始迁的日期，也是占用切换的切点（旧床让到这天，新床从这天起占） */
    @Column(name = "plan_start_date", nullable = false)
    public LocalDate planStartDate;

    /** 计划哪天迁完 */
    @Column(name = "plan_end_date", nullable = false)
    public LocalDate planEndDate;

    /** 已落账 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
