package lk.nsbm.university.service;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.UserRepository;
import lk.nsbm.university.util.ContactNumberFormatter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String credential) throws UsernameNotFoundException {
        User user = resolveUserByCredential(credential);

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

    private User resolveUserByCredential(String credential) {
        String trimmed = StringUtils.hasText(credential) ? credential.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            throw new UsernameNotFoundException("Username is required");
        }
        Optional<User> byStudentId = userRepository.findByStudentId(trimmed);
        if (byStudentId.isPresent()) {
            return byStudentId.get();
        }
        String normalizedContact = ContactNumberFormatter.normalize(trimmed);
        if (StringUtils.hasText(normalizedContact)) {
            return userRepository.findByContactNumber(normalizedContact)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with the provided credentials"));
        }
        throw new UsernameNotFoundException("User not found with the provided credentials");
    }
}
