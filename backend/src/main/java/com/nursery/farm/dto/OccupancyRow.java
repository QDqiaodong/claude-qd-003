package com.nursery.farm.dto;

import java.time.LocalDate;

/** 苗床占用账上的一行：一段占用，附上批次/苗床/温室/品种的名字，前端直接展示。 */
public class OccupancyRow {

    public Long id;
    public Long batchId;
    public String batchNo;
    public Integer planQty;
    public String batchStatus;
    public String grower;
    public Long seedbedId;
    public String seedbedCode;
    public String seedbedName;
    public String seedbedStatus;
    public Integer capacity;
    public Long greenhouseId;
    public String greenhouseName;
    public Long varietyId;
    public String varietyName;
    public LocalDate fromDate;
    public LocalDate toDate;
    /** true 表示还占着（to_date 为空） */
    public boolean open;
}
