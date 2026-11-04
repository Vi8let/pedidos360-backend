package cl.duoc.pedidos360_backend.service;

import cl.duoc.pedidos360_backend.exception.ResourceNotFoundException;
import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Capa de servicio encargada de la logica de negocio para la gestion de Pedidos.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    // Inyeccion de dependencias por constructor (mejor practica recomendada por Spring)
    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Obtiene el listado completo de pedidos registrados en el sistema.
     * @return Lista de pedidos.
     */
    @Transactional(readOnly = true)
    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    /**
     * Busca un pedido especifico por su identificador unico.
     * @param id Identificador del pedido.
     * @return El pedido encontrado.
     * @throws ResourceNotFoundException Si el pedido con el ID especificado no existe.
     */
    @Transactional(readOnly = true)
    public Pedido obtenerPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
    }

    /**
     * Registra un nuevo pedido en la base de datos aplicando validaciones de negocio:
     * 1. Valida que 'imageUrl' no venga nula ni vacia.
     * 2. Asigna un estado inicial por defecto ("PENDIENTE") si no fue especificado.
     *
     * @param pedido Datos del pedido a registrar.
     * @return El pedido persistido con su ID generado.
     */
    @Transactional
    public Pedido crearPedido(Pedido pedido) {
        // Validacion: imageUrl no debe venir vacia
        if (pedido.getImageUrl() == null || pedido.getImageUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("La URL de la imagen (imageUrl) es obligatoria y no puede estar vacía.");
        }

        // Asignacion de estado inicial si no lo trae
        if (pedido.getEstado() == null || pedido.getEstado().trim().isEmpty()) {
            pedido.setEstado("PENDIENTE");
        }

        return pedidoRepository.save(pedido);
    }

    /**
     * Actualiza la informacion de un pedido existente.
     *
     * @param id Identificador del pedido a actualizar.
     * @param detalles Nuevos datos para el pedido (producto, stock, cantidad, estado, imageUrl).
     * @return El pedido actualizado y persistido.
     * @throws ResourceNotFoundException Si el ID del pedido no existe.
     */
    @Transactional
    public Pedido actualizarPedido(Long id, Pedido detalles) {
        Pedido pedidoExistente = obtenerPorId(id);

        if (detalles.getProducto() != null && !detalles.getProducto().trim().isEmpty()) {
            pedidoExistente.setProducto(detalles.getProducto().trim());
        }

        if (detalles.getImageUrl() != null && !detalles.getImageUrl().trim().isEmpty()) {
            pedidoExistente.setImageUrl(detalles.getImageUrl().trim());
        }

        pedidoExistente.setCantidad(detalles.getCantidad());
        pedidoExistente.setStock(detalles.getStock());

        if (detalles.getEstado() != null && !detalles.getEstado().trim().isEmpty()) {
            pedidoExistente.setEstado(detalles.getEstado().trim());
        }

        return pedidoRepository.save(pedidoExistente);
    }

    /**
     * Elimina un pedido del sistema por su ID.
     *
     * @param id Identificador del pedido a eliminar.
     * @throws ResourceNotFoundException Si el ID del pedido no existe.
     */
    @Transactional
    public void eliminarPedido(Long id) {
        Pedido pedidoExistente = obtenerPorId(id);
        pedidoRepository.delete(pedidoExistente);
    }
}
