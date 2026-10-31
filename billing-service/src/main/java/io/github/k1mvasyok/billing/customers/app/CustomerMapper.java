package io.github.k1mvasyok.billing.customers.app;

import io.github.k1mvasyok.billing.customers.api.CustomerResponse;
import io.github.k1mvasyok.billing.customers.domain.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);
}
