package com.drogia.modules.inventario.repository;

import com.drogia.modules.inventario.entity.InventarioItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de inventario. */
public interface InventarioRepository extends JpaRepository<InventarioItem, Long> {

    List<InventarioItem> findAllByOrderByIdAsc();

    Optional<InventarioItem> findByProductAndLot(String product, String lot);

    List<InventarioItem> findAllByPurchaseOrderCode(String purchaseOrderCode);
}
