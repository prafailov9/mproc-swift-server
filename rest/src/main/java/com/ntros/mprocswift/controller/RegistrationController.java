package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.dto.UserRegistrationDTO;
import com.ntros.mprocswift.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
public class RegistrationController extends AbstractApiController {

    private final UserService userService;

    @Autowired
    public RegistrationController(final UserService userService) {
        this.userService = userService;
    }


    public ResponseEntity<?> registerUser(@Validated @RequestBody UserRegistrationDTO userRegistrationDTO) {
        return null;
    }



}
