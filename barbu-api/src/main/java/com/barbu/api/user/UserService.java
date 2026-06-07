package com.barbu.api.user;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @throws UsernameNotFoundException if no user with the given username exists
     */
    public UserEntity findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(()->
                new UsernameNotFoundException("User with username " + username + " not found!"));
    }

    public UserProfileResponse getProfileByUsername(String username) {
        UserEntity user = findByUsername(username);
        return new UserProfileResponse(user.getId(), user.getUsername());
    }
}