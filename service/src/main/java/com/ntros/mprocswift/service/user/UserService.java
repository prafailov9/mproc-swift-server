package com.ntros.mprocswift.service.user;

import com.ntros.mprocswift.dto.UserRegistrationDTO;
import com.ntros.mprocswift.model.User;

import java.util.List;

public interface UserService {

    User processRegistration(final UserRegistrationDTO userRegistrationDTO);

    User getUser(final int userId);

    List<User> getAllUsers();

    User getUserByUsername(final String username);

    User createUser(final User user);
}
