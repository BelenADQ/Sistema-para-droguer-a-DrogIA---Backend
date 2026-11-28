package com.drogia.modules.ventas.entity;

import com.drogia.modules.ventas.entity.enums.EstadoVenta;
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

/** Venta generada desde una cotización aceptada. */
@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "cotizacion_id")
    private Long cotizacionId;

    @Column(name = "cotizacion_numero", length = 30)
    private String cotizacionNumero;

    @Column(name = "caso_id")
    private Long casoId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cliente_json", columnDefinition = "TEXT")
    private String clienteJson;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "igv")
    private BigDecimal igv;

    @Column(name = "total")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoVenta estado;

    @Column(name = "creado_por", length = 120)
    private String creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;
}
