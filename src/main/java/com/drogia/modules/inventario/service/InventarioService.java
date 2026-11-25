package com.drogia.modules.inventario.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.inventario.dto.request.InventarioRequest;
import com.drogia.modules.inventario.dto.response.InventarioResponse;
import com.drogia.modules.inventario.entity.InventarioItem;
import com.drogia.modules.inventario.repository.InventarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de inventario y recepción de lotes. */
@Service
@RequiredArgsConstructor
@Transactional
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    public List<InventarioResponse> listar() {
        return inventarioRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public InventarioResponse crear(InventarioRequest request) {
        return toResponse(registrar(request));
    }

    public InventarioResponse actualizar(Long id, InventarioRequest request) {
        InventarioItem item = buscar(id);
        item.setProductoId(request.productoId());
        item.setProduct(request.product());
        item.setPurchaseOrderCode(request.purchaseOrderCode());
        item.setLot(request.lot());
        item.setStock(request.stock() == null ? 0 : request.stock());
        item.setMinimumStock(request.minimumStock() == null ? 10 : request.minimumStock());
        item.setExpiration(request.expiration() == null ? "2027-12-31" : request.expiration());
        return toResponse(inventarioRepository.save(item));
    }

    /** Crea o incrementa un lote al recibir una orden de compra. */
    public InventarioItem registrar(InventarioRequest request) {
        String lote = request.lot() == null || request.lot().isBlank()
                ? "LOT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()
                : request.lot().trim();
        InventarioItem item = inventarioRepository
                .findByProductAndLot(request.product().trim(), lote)
                .orElseGet(() -> InventarioItem.builder()
                        .productoId(request.productoId())
                        .product(request.product().trim())
                        .purchaseOrderCode(request.purchaseOrderCode())
                        .lot(lote)
                        .stock(0)
                        .minimumStock(request.minimumStock() == null ? 10 : request.minimumStock())
                        .expiration(request.expiration() == null ? "2027-12-31" : request.expiration())
                        .build());
        item.setStock(item.getStock() + (request.stock() == null ? 0 : request.stock()));
        if (request.purchaseOrderCode() != null && !request.purchaseOrderCode().isBlank()) {
            item.setPurchaseOrderCode(request.purchaseOrderCode());
        }
        return inventarioRepository.save(item);
    }

    private InventarioItem buscar(Long id) {
        return inventarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item de inventario no encontrado"));
    }

    private InventarioResponse toResponse(InventarioItem item) {
        return new InventarioResponse(
                item.getId(), item.getProductoId(), item.getProduct(), item.getPurchaseOrderCode(),
                item.getLot(), item.getStock(), item.getMinimumStock(), item.getExpiration());
    }
}
