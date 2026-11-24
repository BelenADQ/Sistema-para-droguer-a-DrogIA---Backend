package com.drogia.modules.clientes.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.clientes.dto.request.ClienteRequest;
import com.drogia.modules.clientes.dto.response.ClienteResponse;
import com.drogia.modules.clientes.entity.Cliente;
import com.drogia.modules.clientes.repository.ClienteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de gestión de clientes. */
@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;

    /** Lista los clientes ordenados por identificador. */
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Devuelve un cliente por su identificador. */
    public ClienteResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Crea un cliente después de validar su RUC. */
    public ClienteResponse crear(ClienteRequest request) {
        String document = validarDocumento(request.document(), null);
        Cliente cliente = Cliente.builder()
                .name(request.name().trim())
                .document(document)
                .build();
        return toResponse(clienteRepository.save(cliente));
    }

    /** Actualiza los datos de un cliente. */
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        String document = validarDocumento(request.document(), id);
        cliente.setName(request.name().trim());
        cliente.setDocument(document);
        return toResponse(clienteRepository.save(cliente));
    }

    /** Elimina un cliente existente. */
    public void eliminar(Long id) {
        clienteRepository.delete(buscar(id));
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    private String validarDocumento(String document, Long id) {
        if (document == null || document.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El documento es obligatorio");
        }
        String value = document.trim();
        if (!value.matches("\\d{11}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El documento debe tener 11 dígitos");
        }
        clienteRepository.findByDocument(value)
                .filter(otro -> id == null || !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "El RUC ya está registrado");
                });
        return value;
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getName(), cliente.getDocument());
    }
}
