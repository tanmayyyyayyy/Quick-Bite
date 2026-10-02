package com.quickbite.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.api.entity.Address;
import com.quickbite.api.entity.User;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.AddressRepository;
import com.quickbite.api.repository.OrderRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.AddressCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository, orderRepository, userRepository);
    }

    @Test
    void firstAddressBecomesDefault() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(new User()));
        when(addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(4L)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address address = addressService.create(4L, command());

        assertThat(address.isDefaultAddress()).isTrue();
        assertThat(address.getCountry()).isEqualTo("US");
    }

    @Test
    void cannotDeleteAddressOwnedByAnotherUser() {
        when(addressRepository.findByIdAndUserId(10L, 4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(4L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).delete(any(Address.class));
    }

    private static AddressCommand command() {
        return new AddressCommand("Home", "Alex Doe", "+1 555 0100", "1 Main St", null,
                "Oakland", "CA", "94612", "us", false);
    }
}
