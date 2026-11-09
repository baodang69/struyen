package com.struyen.struyen.security;

import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findWithRolesByUsername(usernameOrEmail)
                .or(() -> userRepository.findWithRolesByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung voi username hoac email: " + usernameOrEmail));

        return new CustomUserDetails(user);
    }
}
