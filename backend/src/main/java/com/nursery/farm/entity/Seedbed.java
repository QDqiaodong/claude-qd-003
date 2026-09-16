package com.nursery.farm.entity;

import jakarta.persistence.*;

/** 苗床：编号唯一，归属某个温室，有可放株数上限。 */
@Entity
@Table(name = "seedbed")
public class Seedbed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    @Column(name = "greenhouse_id")
    public Long greenhouseId;

    /** 可放株数 */
    @Column(nullable = false)
    public Integer capacity;

    /** 在用 / 空置 / 维修 */
    @Column(nullable = false, length = 16)
    public String status;
}
