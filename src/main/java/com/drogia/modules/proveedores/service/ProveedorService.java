package com.drogia.modules.proveedores.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.proveedores.dto.request.ProveedorRequest;
import com.drogia.modules.proveedores.dto.response.ProveedorResponse;
import com.drogia.modules.proveedores.entity.Proveedor;
import com.drogia.modules.proveedores.repository.ProveedorRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de gestión de proveedores. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    /** Lista los proveedores ordenados por identificador. */
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Devuelve un proveedor por su identificador. */
    public ProveedorResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Crea un proveedor después de validar su RUC. */
    public ProveedorResponse crear(ProveedorRequest request) {
        String document = validarDocumento(request.document(), null);
        Proveedor proveedor = Proveedor.builder()
                .name(request.name().trim())
                .document(document)
                .phone(request.phone().trim())
                .email(request.email().trim())
                .build();
        return toResponse(proveedorRepository.save(proveedor));
    }

    /** Actualiza los datos de un proveedor. */
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscar(id);
        String document = validarDocumento(request.document(), id);
        proveedor.setName(request.name().trim());
        proveedor.setDocument(document);
        proveedor.setPhone(request.phone().trim());
        proveedor.setEmail(request.email().trim());
        return toResponse(proveedorRepository.save(proveedor));
    }

    /** Elimina un proveedor existente. */
    public void eliminar(Long id) {
        proveedorRepository.delete(buscar(id));
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
    }

    private String validarDocumento(String document, Long id) {
        if (document == null || document.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El documento es obligatorio");
        }
        String value = document.trim();
        if (!value.matches("\\d{11}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El documento debe tener 11 dígitos");
        }
        proveedorRepository.findByDocument(value)
                .filter(otro -> id == null || !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "El RUC ya está registrado");
                });
        return value;
    }

    private ProveedorResponse toResponse(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getName(),
                proveedor.getDocument(),
                proveedor.getPhone(),
                proveedor.getEmail());
    }
}
