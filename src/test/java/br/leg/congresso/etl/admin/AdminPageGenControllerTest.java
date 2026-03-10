package br.leg.congresso.etl.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.leg.congresso.etl.pagegen.PageGeneratorService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPageGenController")
class AdminPageGenControllerTest {

    @Mock
    PageGeneratorService pageGeneratorService;

    @InjectMocks
    AdminPageGenController controller;

    @Nested
    @DisplayName("POST /generate")
    class Generate {

        @Test
        @DisplayName("retorna 202 Accepted ao iniciar geração (sem filtro)")
        void retorna202() {
            var response = controller.generatePages(null);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        @Test
        @DisplayName("retorna 202 Accepted ao iniciar geração com filtro de ano")
        void retorna202ComAno() {
            var response = controller.generatePages(2024);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        @Test
        @DisplayName("resposta contém mensagem e status=iniciado")
        void retornaMensagem() {
            var body = controller.generatePages(null).getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("status")).isEqualTo("iniciado");
            assertThat(body.get("mensagem").toString()).contains("background");
        }

        @Test
        @DisplayName("retorna 409 Conflict se já estiver em andamento")
        void retorna409SeEmAndamento() throws InterruptedException {
            // bloqueia a thread async para simular geração em andamento
            CountDownLatch inicio = new CountDownLatch(1);
            CountDownLatch fim = new CountDownLatch(1);
            when(pageGeneratorService.generateAll((Integer) null)).thenAnswer(inv -> {
                inicio.countDown();
                fim.await(2, TimeUnit.SECONDS);
                return 0;
            });

            // primeira chamada — deve aceitar e iniciar async
            var primeira = controller.generatePages(null);
            assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

            // espera a task async marcar running=true
            inicio.await(2, TimeUnit.SECONDS);

            // segunda chamada — deve recusar com 409
            var segunda = controller.generatePages(null);
            assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(segunda.getBody()).containsKey("status");

            fim.countDown();
        }
    }

    @Nested
    @DisplayName("GET /status")
    class Status {

        @Test
        @DisplayName("retorna status=ocioso quando não está gerando")
        void statusOcioso() {
            var response = controller.status();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("status")).isEqualTo("ocioso");
            assertThat(body.get("emAndamento")).isEqualTo(false);
        }
    }
}
