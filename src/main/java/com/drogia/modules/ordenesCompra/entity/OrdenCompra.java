package com.drogia.modules.ordenesCompra.entity;

import com.drogia.modules.ordenesCompra.entity.enums.EstadoOrdenCompra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Orden de compra gestionada por el área de Compras. */
@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "proveedor", length = 180)
    private String proveedor;

    @Column(name = "caso_id")
    private Long casoId;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "venta_numero", length = 30)
    private String ventaNumero;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "items_json", columnDefinition = "LONGTEXT")
    private String itemsJson;

    @Column(name = "solicitudes_json", columnDefinition = "LONGTEXT")
    private String solicitudesJson;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "igv")
    private BigDecimal igv;

    @Column(name = "total")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoOrdenCompra estado;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "creado_por", length = 120)
    private String creadoPor;
}
