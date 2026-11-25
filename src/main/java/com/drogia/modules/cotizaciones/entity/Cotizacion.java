package com.drogia.modules.cotizaciones.entity;

import com.drogia.modules.cotizaciones.entity.enums.EstadoCotizacion;
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

/** Cotización creada por Ventas a partir de un caso de correo. */
@Entity
@Table(name = "cotizaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "caso_id", unique = true)
    private Long casoId;

    @Column(name = "correo_id")
    private Long correoId;

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "creado_por_nombre", length = 120)
    private String creadoPorNombre;

    @Column(name = "cliente", length = 200)
    private String cliente;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "condiciones_pago", length = 255)
    private String condicionesPago;

    @Column(name = "vigencia", length = 100)
    private String vigencia;

    @Column(name = "total")
    private java.math.BigDecimal total;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "cliente_json", columnDefinition = "LONGTEXT")
    private String clienteJson;

    @Column(name = "destino_json", columnDefinition = "LONGTEXT")
    private String destinoJson;

    @Column(name = "proveedores_json", columnDefinition = "LONGTEXT")
    private String proveedoresJson;

    @Column(name = "codigo_solicitud", length = 40)
    private String codigoSolicitud;

    @Column(name = "subtotal")
    private java.math.BigDecimal subtotal;

    @Column(name = "igv")
    private java.math.BigDecimal igv;

    @Column(name = "fecha_limite_cotizacion")
    private LocalDateTime fechaLimiteCotizacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
