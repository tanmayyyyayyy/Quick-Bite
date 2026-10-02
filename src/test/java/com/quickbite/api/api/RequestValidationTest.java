package com.quickbite.api.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickbite.api.api.dto.CartItemRequest;
import com.quickbite.api.api.dto.MenuItemRequest;
import com.quickbite.api.api.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsInvalidRegistrationFields() {
        RegisterRequest request = new RegisterRequest("not-an-email", "short", "", "", "telephone");

        Set<String> invalidFields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(invalidFields).contains("email", "password", "firstName", "lastName", "phone");
    }

    @Test
    void rejectsNonPositiveMenuPrice() {
        MenuItemRequest request = new MenuItemRequest("Soup", null, "Lunch", BigDecimal.ZERO, true);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("price");
    }

    @Test
    void rejectsCartQuantitiesOverTheLimit() {
        CartItemRequest request = new CartItemRequest(1L, 100);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("quantity");
    }
}
