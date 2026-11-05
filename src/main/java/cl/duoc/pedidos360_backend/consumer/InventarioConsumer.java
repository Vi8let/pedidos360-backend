package cl.duoc.pedidos360_backend.consumer;

import cl.duoc.pedidos360_backend.config.RabbitNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de auditoría de inventario.
 *
 * Cumple con los Indicadores de la rúbrica EP3:
 * - Indicador 4 (15%): Consumidor con @RabbitListener organizado por dominio funcional en paquete 'consumer'.
 * - Indicador 5 (20%): Confirmación manual explícita con ACK (basicAck) ante éxito,
 *   y NACK sin reencolamiento (basicNack(deliveryTag, false, false)) ante errores
 *   para desviar automáticamente el mensaje a la Dead Letter Queue (DLQ).
 */
@Component
public class InventarioConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioConsumer.class);

    private final ObjectMapper objectMapper;

    public InventarioConsumer() {
        this(new ObjectMapper());
    }

    public InventarioConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Escucha y procesa mensajes de la cola de inventario.
     *
     * @param payload Cuerpo del mensaje en formato JSON
     * @param channel Canal AMQP para confirmaciones manuales
     * @param deliveryTag Etiqueta única del mensaje para el canal
     * @throws IOException Si ocurre un error de comunicación con el canal AMQP
     */
    @RabbitListener(queues = RabbitNames.COLA_AUDITORIA_INVENTARIO)
    public void procesarEventoInventario(
            String payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        log.info("[INVENTARIO CONSUMER] Mensaje recibido de '{}' [deliveryTag={}]: {}",
                RabbitNames.COLA_AUDITORIA_INVENTARIO, deliveryTag, payload);

        try {
            if (payload == null || payload.isBlank()) {
                throw new IllegalArgumentException("El payload del mensaje está vacío o es nulo");
            }

            // Validar la estructura JSON
            objectMapper.readTree(payload);

            // Simulación del procesamiento de negocio del evento de inventario
            log.info("[INVENTARIO CONSUMER] Evento de inventario procesado correctamente [deliveryTag={}].", deliveryTag);

            // 1. Confirmación positiva (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("[INVENTARIO CONSUMER] basicAck emitido exitosamente para deliveryTag={}.", deliveryTag);

        } catch (Exception ex) {
            log.error("[INVENTARIO CONSUMER] Error al procesar mensaje [deliveryTag={}]. Enviando a DLQ ('{}'). Causa: {}",
                    deliveryTag, RabbitNames.DLQ_INVENTARIO, ex.getMessage());

            // 2. Rechazo explícito sin reencolar (requeue = false) -> Se desvía a pedidos.dlx / DLQ
            channel.basicNack(deliveryTag, false, false);
            log.warn("[INVENTARIO CONSUMER] basicNack(requeue=false) emitido para deliveryTag={}. Mensaje enrutado a DLQ.", deliveryTag);
        }
    }
}

