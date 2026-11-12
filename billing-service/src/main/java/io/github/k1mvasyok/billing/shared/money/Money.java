package io.github.k1mvasyok.billing.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Деньги на входе: не больше двух знаков после запятой (BR-01). Хвостовые нули допустимы. */
public final class Money {

    private static final int SCALE = 2;

    private Money() {
    }

    /**
     * Приводит сумму к копейкам. Дробная часть тоньше копейки — ошибка, а не повод округлить:
     * молчаливое округление расходится с тем, что клиент отправил, и с тем, что видно в ответе.
     */
    public static BigDecimal toCents(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() > SCALE) {
            throw new IllegalArgumentException(
                    "Сумма не может содержать дробную часть меньше копейки: " + amount.toPlainString());
        }
        return stripped.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
