package com.nazkord.siemajero.controllers;

import com.nazkord.siemajero.model.User;
import com.nazkord.siemajero.security.Role;
import com.nazkord.siemajero.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<User> getAllUsers(SecurityContextHolderAwareRequestWrapper securityWrapper,
                                  @RequestParam(required = false) String userName) {

        if(userName == null) { // if userName doesnt exist
            if (securityWrapper.isUserInRole(Role.ADMIN.name())) { // get all bets if admin
                return userService.getAllUsers();
            } else {
                return null;
            }
        } else { // if exist
            if(isOperationPermitted(userService.getUserByName(userName).getId(), securityWrapper)) {
                User user = userService.getUserByName(userName);
                return Collections.singletonList(user);
            } else {
                return null;
            }
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{userId}")
    public User getUser(@PathVariable Long userId, SecurityContextHolderAwareRequestWrapper securityWrapper) {

        if (isOperationPermitted(userId, securityWrapper)) {
            return userService.getUserById(userId);
        } else {
            return null;
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> postUser(@RequestBody User newUser, SecurityContextHolderAwareRequestWrapper securityWrapper) {
        if (securityWrapper.isUserInRole(Role.ADMIN.name())) {
            try {
                if(userService.isUniqueName(newUser.getName())) {
                    userService.addUser(newUser);
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    String errorMessage = "Not UNIQUE name <== error";
                    return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
                }
            } catch (Exception e) {
                String errorMessage = e + " <== error";
                return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Forbidden", HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/tokenSignIn")
    public ResponseEntity<?> verifyToken(@RequestBody String tokenId) {
        return null;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody User userToUpdate, SecurityContextHolderAwareRequestWrapper securityWrapper) {
        try {
            if (isOperationPermitted(userId, securityWrapper)) {
                userToUpdate.setId(userId);
                userService.updateUser(userToUpdate);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>("bad permission <== error", HttpStatus.METHOD_NOT_ALLOWED);
            }
        } catch (Exception e) {
            String errorMessage = "Error while updating <== error";
            return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{userId}")
    public void deleteUser(@PathVariable Long userId, SecurityContextHolderAwareRequestWrapper securityWrapper) {
        if (securityWrapper.isUserInRole(String.valueOf(Role.ADMIN))) {
            userService.deleteUser(userId);
        }
    }

    private boolean isOperationPermitted(Long userIdToCheck, SecurityContextHolderAwareRequestWrapper securityWrapper) {
        if (securityWrapper.isUserInRole(Role.ADMIN.name())) {
            return true;
        }
        // check whether the logged user want to get his own profile (getRemoteUser return name)
        User currentUser = userService.getUserByName(securityWrapper.getRemoteUser());
        return currentUser.getId().equals(userIdToCheck);
    }

}

