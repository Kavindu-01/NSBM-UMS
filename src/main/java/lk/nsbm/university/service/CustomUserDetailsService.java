package lk.nsbm.university.service;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with student ID: " + studentId));

        if (user.getAccountStatus() != AccountStatus.APPROVED) {
            throw new UsernameNotFoundException("Account is not approved yet");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getStudentId())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    public User getUserByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with student ID: " + studentId));
    }
}
