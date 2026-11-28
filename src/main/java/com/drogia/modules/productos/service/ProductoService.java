package com.drogia.modules.productos.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.productos.dto.request.ProductoRequest;
import com.drogia.modules.productos.dto.response.ProductoResponse;
import com.drogia.modules.productos.entity.Producto;
import com.drogia.modules.productos.repository.ProductoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio del catálogo de productos. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;

    /** Lista todos los productos o los que coinciden con la búsqueda. */
    public List<ProductoResponse> listar(String busqueda) {
        List<Producto> productos;
        if (busqueda == null || busqueda.isBlank()) {
            productos = productoRepository.findAll(Sort.by("id"));
        } else {
            String valor = "%" + busqueda.trim() + "%";
            productos = productoRepository
                    .findByNameContainingIgnoreCaseOrLotContainingIgnoreCaseOrHealthRegistrationContainingIgnoreCase(
                            valor, valor, valor);
        }
        return productos.stream().map(this::toResponse).toList();
    }

    public ProductoResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = Producto.builder()
                .name(request.name().trim())
                .unit(request.unit() == null ? "UNIDAD" : request.unit().trim())
                .lot(request.lot())
                .healthRegistration(request.healthRegistration())
                .price(request.price())
                .stock(request.stock() == null ? 0 : request.stock())
                .expiration(request.expiration())
                .build();
        return toResponse(productoRepository.save(producto));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        producto.setName(request.name().trim());
        producto.setUnit(request.unit() == null ? "UNIDAD" : request.unit().trim());
        producto.setLot(request.lot());
        producto.setHealthRegistration(request.healthRegistration());
        producto.setPrice(request.price());
        producto.setStock(request.stock() == null ? 0 : request.stock());
        producto.setExpiration(request.expiration());
        return toResponse(productoRepository.save(producto));
    }

    public void eliminar(Long id) {
        productoRepository.delete(buscar(id));
    }

    /** Devuelve la entidad de un producto para otros módulos. */
    public Producto obtenerEntidad(Long id) {
        return buscar(id);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(), producto.getName(), producto.getUnit(), producto.getLot(),
                producto.getHealthRegistration(), producto.getPrice(), producto.getStock(),
                producto.getExpiration());
    }
}
