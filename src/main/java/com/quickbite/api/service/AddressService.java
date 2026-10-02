package com.quickbite.api.service;

import com.quickbite.api.entity.Address;
import com.quickbite.api.entity.User;
import com.quickbite.api.exception.ConflictException;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.AddressRepository;
import com.quickbite.api.repository.OrderRepository;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.AddressCommand;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Address> getForUser(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
    }

    @Transactional
    public Address create(Long userId, AddressCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Address address = new Address();
        address.setUser(user);
        apply(userId, address, command);
        return addressRepository.save(address);
    }

    @Transactional
    public Address update(Long userId, Long addressId, AddressCommand command) {
        Address address = findOwned(userId, addressId);
        apply(userId, address, command);
        return address;
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        if (orderRepository.existsByAddressId(addressId)) {
            throw new ConflictException("An address used by an order cannot be deleted");
        }
        addressRepository.delete(address);
    }

    private Address findOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    private void apply(Long userId, Address address, AddressCommand command) {
        List<Address> existing = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
        if (command.defaultAddress()) {
            existing.stream().filter(other -> !other.getId().equals(address.getId()))
                    .forEach(other -> other.setDefaultAddress(false));
        }
        address.setLabel(command.label().trim());
        address.setRecipientName(command.recipientName().trim());
        address.setPhone(command.phone().trim());
        address.setAddressLine1(command.addressLine1().trim());
        address.setAddressLine2(command.addressLine2());
        address.setCity(command.city().trim());
        address.setRegion(command.region());
        address.setPostalCode(command.postalCode().trim());
        address.setCountry(command.country().trim().toUpperCase());
        address.setDefaultAddress(command.defaultAddress() || existing.isEmpty());
    }
}