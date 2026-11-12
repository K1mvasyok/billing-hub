package io.github.k1mvasyok.billing.contracts.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Деньги приходят строкой ("1234.50"), как сказано в плане: число в JSON теряет точность у клиентов на JavaScript.
 * validTo = null — цена действует без даты окончания.
 */
public record PriceRequest(
        @NotNull LocalDate validFrom,
        @Schema(description = "Дата окончания. Не входит в срок: последний день действия — предыдущий день.")
        LocalDate validTo,
        @NotNull @PositiveOrZero @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal monthlyPrice
) {
}
