package cl.duoc.pedidos360_backend.consumer;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias para InventarioConsumer.
 * Verifica la confirmación explícita mediante ACK/NACK (Indicadores 4 y 5 de la rúbrica EP3).
 */
@ExtendWith(MockitoExtension.class)
class InventarioConsumerTest {

    @Mock
    private Channel channel;

    private InventarioConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventarioConsumer(new ObjectMapper());
    }

    @Test
    void procesarEventoInventario_conPayloadValido_emiteBasicAck() throws IOException {
        String payloadValido = """
                {
                    "eventoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "routingKey": "inventario.producto.creado",
                    "accion": "CREACION",
                    "productoId": 1,
                    "producto": "Servidor HP ProLiant",
                    "cantidad": 5,
                    "stock": 20,
                    "estado": "DISPONIBLE"
                }
                """;
        long deliveryTag = 101L;

        consumer.procesarEventoInventario(payloadValido, channel, deliveryTag);

        // Verifica que se confirme exitosamente con basicAck
        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void procesarEventoInventario_conPayloadInvalido_emiteBasicNackSinReencolar() throws IOException {
        String payloadInvalido = "ESTO_NO_ES_UN_JSON_VALIDO { [";
        long deliveryTag = 202L;

        consumer.procesarEventoInventario(payloadInvalido, channel, deliveryTag);

        // Verifica que ante error se emita basicNack con requeue=false hacia la DLQ
        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void procesarEventoInventario_conPayloadVacio_emiteBasicNackSinReencolar() throws IOException {
        long deliveryTag = 303L;

        consumer.procesarEventoInventario("", channel, deliveryTag);

        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}

