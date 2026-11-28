package com.drogia.modules.despachos.entity;

import com.drogia.modules.despachos.entity.enums.EstadoDespacho;
import com.drogia.modules.pedidosAlmacen.entity.enums.TipoDespacho;
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

/** Despacho local o mediante empresa courier. */
@Entity
@Table(name = "despachos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "caso_id")
    private Long casoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_despacho", nullable = false, length = 20)
    private TipoDespacho tipoDespacho;

    @Column(name = "third_party_name", length = 180)
    private String thirdPartyName;

    @Column(name = "third_party_doc", length = 20)
    private String thirdPartyDoc;

    @Column(name = "courier_empresa", length = 120)
    private String courierEmpresa;

    @Column(name = "courier_servicio", length = 120)
    private String courierServicio;

    @Column(name = "numero_guia", length = 60)
    private String numeroGuia;

    @Column(name = "costo_envio")
    private BigDecimal costoEnvio;

    @Column(name = "product", length = 180)
    private String product;

    @Column(name = "purchase_order_code", length = 30)
    private String purchaseOrderCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EstadoDespacho status;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
