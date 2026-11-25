package com.drogia.modules.pedidosAlmacen.entity;

import com.drogia.modules.pedidosAlmacen.entity.enums.EstadoPedidoAlmacen;
import com.drogia.modules.pedidosAlmacen.entity.enums.TipoDespacho;
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

/** Pedido en almacén que debe ser tomado y verificado por un almacenero. */
@Entity
@Table(name = "pedidos_almacen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "orden_compra_id")
    private Long ordenCompraId;

    @Column(name = "caso_id")
    private Long casoId;

    @Column(name = "cliente_json", columnDefinition = "LONGTEXT")
    private String clienteJson;

    @Column(name = "productos_json", columnDefinition = "LONGTEXT")
    private String productosJson;

    @Column(name = "destino_json", columnDefinition = "LONGTEXT")
    private String destinoJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_despacho", nullable = false, length = 20)
    private TipoDespacho tipoDespacho;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedidoAlmacen estado;

    @Column(name = "asignado_a_id")
    private Long asignadoAId;

    @Column(name = "asignado_a_nombre", length = 120)
    private String asignadoANombre;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "verificado_info")
    private boolean verificadoInfo;

    @Column(name = "verificado_lotes")
    private boolean verificadoLotes;

    @Column(name = "despacho_id")
    private Long despachoId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
