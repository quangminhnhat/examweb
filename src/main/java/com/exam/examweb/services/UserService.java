package com.exam.examweb.services;

import com.exam.examweb.constants.Provider;
import com.exam.examweb.entities.Role;
import com.exam.examweb.entities.User;
import com.exam.examweb.repositories.IRoleRepository;
import com.exam.examweb.repositories.IUserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements UserDetailsService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IRoleRepository roleRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void save(@NotNull User user, String roleName) {
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        user.setProvider(Provider.LOCAL.value);

        Long roleId = com.exam.examweb.constants.Role.student.value; // Default to student
        if ("teacher".equalsIgnoreCase(roleName)) {
            roleId = com.exam.examweb.constants.Role.teacher.value;
        }
        
        Role role = roleRepository.findRoleById(roleId);
        if (role != null) {
            user.getRoles().add(role);
        }
        
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public Optional<User> findByUsername(String username) throws
            UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }

    public void saveOauthUser(String email, @NotNull String username, String googleId, String fullName, String avatar) {
        if(userRepository.findByGoogleId(googleId).isPresent())
            return;
        var user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode(username));
        user.setProvider(Provider.GOOGLE.value);
        user.setGoogleId(googleId);
        user.setFullName(fullName);
        user.setAvatar(avatar);
        
        Role defaultRole = roleRepository.findRoleById(com.exam.examweb.constants.Role.student.value);
        if (defaultRole != null) {
            user.getRoles().add(defaultRole);
        }

        userRepository.save(user);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User updateUserRoles(Long userId, List<String> roleNames) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Clear existing roles
        user.getRoles().clear();

        // Add new roles
        roleNames.stream()
            .map(roleRepository::findByName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(user.getRoles()::add);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }
}
