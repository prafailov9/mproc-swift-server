package com.ntros.mprocswift.service.user;

import com.ntros.mprocswift.dto.UserRegistrationDTO;
import com.ntros.mprocswift.model.Address;
import com.ntros.mprocswift.model.User;
import com.ntros.mprocswift.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class UserDataService implements UserService {

    protected static final Logger log = LoggerFactory.getLogger(UserDataService.class);

    private final UserRepository userRepository;
    private final AddressService addressService;

    @Autowired
    public UserDataService(final UserRepository userRepository,
                           final AddressService addressService) {
        this.userRepository = userRepository;
        this.addressService = addressService;
    }

    @Override
    public User processRegistration(UserRegistrationDTO userRegistrationDTO) {
        Address address = addressService.createAddress(userRegistrationDTO.getAddressDTO());
        return null;
    }

    @Override
    public User getUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    String error = "User not found for id: " + userId;
                    log.error(error);
                    return new RuntimeException(error);
                });
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> {
            String error = "User not found for username: " + username;
            log.error(error);
            return new RuntimeException(error);
        });
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

}
