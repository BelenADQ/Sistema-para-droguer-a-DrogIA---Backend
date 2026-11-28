package com.drogia.modules.casos.entity;

import com.drogia.modules.casos.entity.enums.EstadoCaso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Caso de negocio generado a partir de un correo clasificado por la IA. */
@Entity
@Table(name = "casos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo_id", nullable = false, unique = true)
    private Long correoId;

    @Column(length = 50)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaso estado;

    @Column(name = "asignado_a_id")
    private Long asignadoAId;

    @Column(name = "asignado_a_nombre", length = 120)
    private String asignadoANombre;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "cotizacion_id")
    private Long cotizacionId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "codigo_solicitud", length = 40)
    private String codigoSolicitud;

    @Column(name = "fecha_limite_cotizacion")
    private LocalDateTime fechaLimiteCotizacion;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "orden_compra_id")
    private Long ordenCompraId;

    @Column(name = "pedido_almacen_id")
    private Long pedidoAlmacenId;
}
