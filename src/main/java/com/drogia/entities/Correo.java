package com.drogia.entities;

import com.drogia.entities.enums.EstadoCorreo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Entidad que representa un correo persistido en la tabla correos. */
@Entity
@Table(name = "correos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Correo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String messageId;

    @Column(length = 255)
    private String carpeta;

    @Column(length = 255)
    private String remitente;

    @Column(length = 512)
    private String asunto;

    private LocalDateTime fecha;

    @Column(length = 500)
    private String snippet;

    private boolean leido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCorreo estado;

    @Column(length = 50)
    private String area;

    private LocalDateTime fechaGuardado;

    @Column(name = "cuerpo_texto", columnDefinition = "LONGTEXT")
    private String cuerpoTexto;

    @Column(name = "tipo_clasificacion", length = 40)
    private String tipoClasificacion;

    @Column(name = "codigo_cotizacion_detectado", length = 30)
    private String codigoCotizacionDetectado;

    @Column(name = "resumen_productos", length = 2000)
    private String resumenProductos;

    @Column(name = "error_detalle", length = 2000)
    private String errorDetalle;

    @Column(name = "procesado_at")
    private LocalDateTime procesadoAt;

    @Column(name = "clasificaciones")
    private Integer clasificaciones;
}
