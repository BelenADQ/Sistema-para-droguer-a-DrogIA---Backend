package com.drogia.modules.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lote de producto disponible en almacén. */
@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "product", nullable = false, length = 180)
    private String product;

    @Column(name = "purchase_order_code", length = 30)
    private String purchaseOrderCode;

    @Column(name = "lot", length = 60)
    private String lot;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "minimum_stock")
    private Integer minimumStock;

    @Column(name = "expiration", length = 20)
    private String expiration;
}
