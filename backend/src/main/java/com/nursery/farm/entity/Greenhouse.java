package com.nursery.farm.entity;

import jakarta.persistence.*;

/** 温室：编号唯一，停用后里面的苗床不能再放新批次。 */
@Entity
@Table(name = "greenhouse")
public class Greenhouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 育苗棚 / 成苗棚 / 炼苗棚 */
    @Column(nullable = false, length = 16)
    public String kind;

    /** 在用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
