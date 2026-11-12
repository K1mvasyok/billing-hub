package io.github.k1mvasyok.billing.invoicing.web;

import com.jayway.jsonpath.JsonPath;
import io.github.k1mvasyok.billing.MutableClock;
import io.github.k1mvasyok.billing.TestClockConfiguration;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Закрыть или выставить счета можно только за месяц, который уже закончился: счёт датируется последним днём месяца.
 * Часы переводятся через {@link MutableClock}; после каждого теста они возвращаются к значению по умолчанию.
 * Закрытие блокирует более ранние периоды, поэтому периоды закрытия (2042) и запуска (2043) не пересекаются.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillingPeriodFinishedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    @DisplayName("закрыть будущий месяц нельзя")
    void futureMonthCannotBeClosed() throws Exception {
        clock.setDate(LocalDate.of(2042, 9, 15));

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2042-10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-not-finished"));
    }

    @Test
    @DisplayName("закрыть текущий месяц, который ещё не закончился, нельзя")
    void currentMonthCannotBeClosed() throws Exception {
        clock.setDate(LocalDate.of(2042, 9, 15));

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2042-09"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-not-finished"));
    }

    @Test
    @DisplayName("закрыть прошедший месяц можно")
    void finishedMonthCanBeClosed() throws Exception {
        clock.setDate(LocalDate.of(2042, 9, 15));

        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2042-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("месяц считается закончившимся с первого дня следующего месяца, а не с последнего дня")
    void monthIsFinishedFromFirstDayOfNextMonth() throws Exception {
        clock.setDate(LocalDate.of(2042, 7, 31));
        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2042-07"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-not-finished"));

        clock.setDate(LocalDate.of(2042, 8, 1));
        mockMvc.perform(post("/api/v1/billing-periods/{period}/close", "2042-07"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("запуск выставления за текущий и будущие месяцы отклоняется")
    void runForUnfinishedMonthIsRejected() throws Exception {
        clock.setDate(LocalDate.of(2043, 3, 15));

        for (String period : new String[]{"2043-03", "2043-04"}) {
            mockMvc.perform(post("/api/v1/billing-runs")
                            .contentType(APPLICATION_JSON)
                            .content("{\"period\": \"%s\"}".formatted(period)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:billing-hub:problem:period-not-finished"));
        }
    }

    @Test
    @DisplayName("запуск выставления за прошедший месяц принимается и завершается")
    void runForFinishedMonthIsAccepted() throws Exception {
        clock.setDate(LocalDate.of(2043, 3, 15));

        String runId = JsonPath.read(mockMvc.perform(post("/api/v1/billing-runs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"period\": \"2043-02\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.id");

        String finalStatus = null;
        for (int attempt = 0; attempt < 300 && finalStatus == null; attempt++) {
            String body = mockMvc.perform(get("/api/v1/billing-runs/{id}", runId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String current = JsonPath.read(body, "$.status");
            if (!"RUNNING".equals(current)) {
                finalStatus = current;
            } else {
                Thread.sleep(100);
            }
        }
        assertThat(finalStatus).isEqualTo("COMPLETED");
    }
}
