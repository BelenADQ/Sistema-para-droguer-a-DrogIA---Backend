package com.drogia.modules.pedidosAlmacen.repository;

import com.drogia.modules.pedidosAlmacen.entity.PedidoAlmacen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de pedidos en almacén. */
public interface PedidoAlmacenRepository extends JpaRepository<PedidoAlmacen, Long> {

    Optional<PedidoAlmacen> findByVentaId(Long ventaId);

    Optional<PedidoAlmacen> findByOrdenCompraId(Long ordenCompraId);

    List<PedidoAlmacen> findAll(Sort sort);
}
