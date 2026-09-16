package com.nursery.farm.entity;

import jakarta.persistence.*;

/** 品种：编号唯一，停用后不能再开新批次。 */
@Entity
@Table(name = "variety")
public class Variety {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 草本 / 木本 / 多肉 / 蔬果 */
    @Column(nullable = false, length = 16)
    public String category;

    /** 在售 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
