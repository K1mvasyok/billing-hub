package io.github.k1mvasyok.billing.invoicing.web;

import io.github.k1mvasyok.billing.invoicing.api.BillingPeriodResponse;
import io.github.k1mvasyok.billing.invoicing.app.BillingPeriodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-periods")
public class BillingPeriodController {

    private final BillingPeriodService service;

    public BillingPeriodController(BillingPeriodService service) {
        this.service = service;
    }

    @GetMapping
    List<BillingPeriodResponse> list() {
        return service.list();
    }

    @PostMapping("/{period}/close")
    BillingPeriodResponse close(@PathVariable String period) {
        return service.close(period);
    }
}
