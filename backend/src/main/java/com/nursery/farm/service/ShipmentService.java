package com.nursery.farm.service;

import com.nursery.farm.dto.BizException;
import com.nursery.farm.entity.NurseryBatch;
import com.nursery.farm.entity.Shipment;
import com.nursery.farm.repository.NurseryBatchRepository;
import com.nursery.farm.repository.ShipmentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {

    private final ShipmentRepository shipments;
    private final NurseryBatchRepository batches;

    public ShipmentService(ShipmentRepository shipments, NurseryBatchRepository batches) {
        this.shipments = shipments;
        this.batches = batches;
    }

    public List<Shipment> list(String status, Long batchId) {
        List<Shipment> all = (batchId == null)
                ? shipments.findAllByOrderByUpdatedAtDesc()
                : shipments.findByBatchIdOrderByCreatedAtAsc(batchId);
        return all.stream()
                .filter(s -> status == null || status.isEmpty() || status.equals(s.status))
                .toList();
    }

    private String nextShipmentNo() {
        long n = shipments.count() + 1;
        String no;
        do {
            no = "SH-" + String.format("%04d", n++);
        } while (shipments.existsByShipmentNo(no));
        return no;
    }

    /** 这批苗已经被发掉多少株（退回来的不算）。 */
    private int shippedQty(Long batchId) {
        int sum = 0;
        for (Shipment s : shipments.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            if (!"已退回".equals(s.status)) {
                sum += s.qty;
            }
        }
        return sum;
    }

    @Transactional
    public Shipment open(Shipment input) {
        if (input.batchId == null) {
            throw new BizException("请选一个批次");
        }
        if (input.customer == null || input.customer.isBlank()) {
            throw new BizException("收货方不能为空");
        }
        if (input.qty == null || input.qty <= 0) {
            throw new BizException("发货株数要大于 0");
        }
        NurseryBatch batch = batches.findById(input.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        if (!"待出圃".equals(batch.status) && !"已出圃".equals(batch.status)) {
            throw new BizException("批次 " + batch.batchNo + " 现在是 " + batch.status + "，还不能发货");
        }
        int shipped = shippedQty(batch.id);
        if (shipped + input.qty > batch.actualQty) {
            throw new BizException("批次 " + batch.batchNo + " 实际成苗 " + batch.actualQty
                    + " 株，已经发掉 " + shipped + " 株，这单要发 " + input.qty + " 株不够");
        }

        Shipment saved = new Shipment();
        saved.shipmentNo = nextShipmentNo();
        saved.batchId = batch.id;
        saved.customer = input.customer.trim();
        saved.qty = input.qty;
        saved.carrier = input.carrier;
        saved.status = "待发货";
        saved.createdAt = LocalDateTime.now();
        saved.updatedAt = saved.createdAt;
        return shipments.save(saved);
    }

    /** 待发货 -> 已发货 -> 已签收；已发货还能退回。 */
    @Transactional
    public Shipment advance(Long id, String action, String carrier, LocalDate shipDate) {
        Shipment ship = shipments.findById(id).orElseThrow(() -> new BizException("发货单不存在"));

        if ("ship".equals(action)) {
            if (!"待发货".equals(ship.status)) {
                throw new BizException("只有待发货的单子能发车，这单现在是 " + ship.status);
            }
            if (carrier != null && !carrier.isBlank()) {
                ship.carrier = carrier;
            }
            if (ship.carrier == null || ship.carrier.isBlank()) {
                throw new BizException("发车要写承运人");
            }
            ship.shipDate = shipDate == null ? LocalDate.now() : shipDate;
            ship.status = "已发货";
        } else if ("sign".equals(action)) {
            if (!"已发货".equals(ship.status)) {
                throw new BizException("只有在途的单子能签收，这单现在是 " + ship.status);
            }
            ship.status = "已签收";
        } else if ("back".equals(action)) {
            if (!"已发货".equals(ship.status)) {
                throw new BizException("只有已发货还没签收的单子能退回，这单现在是 " + ship.status);
            }
            ship.status = "已退回";
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        ship.updatedAt = LocalDateTime.now();
        return shipments.save(ship);
    }
}
