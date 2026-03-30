package com.exam.examweb.services;

import com.exam.examweb.constants.Provider;
import com.exam.examweb.entities.Role;
import com.exam.examweb.entities.User;
import com.exam.examweb.payload.RegistrationRequest;
import com.exam.examweb.repositories.IRoleRepository;
import com.exam.examweb.repositories.IUserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' đã tồn tại!");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' đã được sử dụng!");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty() && userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại '" + user.getPhone() + "' đã được đăng ký!");
        }

        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        user.setProvider(Provider.LOCAL.value);

        Long roleId = com.exam.examweb.constants.Role.student.value; 
        if ("teacher".equalsIgnoreCase(roleName)) {
            roleId = com.exam.examweb.constants.Role.teacher.value;
        }
        
        Role role = roleRepository.findRoleById(roleId);
        if (role != null) {
            user.getRoles().add(role);
        }
        
        userRepository.save(user);
    }

    @Transactional
    public String createResetPasswordToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        return token;
    }

    public Optional<User> findByResetPasswordToken(String token) {
        return userRepository.findAll().stream()
                .filter(u -> token.equals(u.getResetPasswordToken()) && 
                             u.getResetPasswordTokenExpiry() != null && 
                             u.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()))
                .findFirst();
    }

    @Transactional
    public void updatePassword(User user, String newPassword) {
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public User registerNewUser(RegistrationRequest registrationRequest) {
        if (userRepository.findByUsername(registrationRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(new BCryptPasswordEncoder().encode(registrationRequest.getPassword()));
        user.setEmail(registrationRequest.getEmail());
        user.setPhone(registrationRequest.getPhone());
        user.setProvider(Provider.LOCAL.value);

        Long roleId = com.exam.examweb.constants.Role.student.value;
        if ("teacher".equalsIgnoreCase(registrationRequest.getRole())) {
            roleId = com.exam.examweb.constants.Role.teacher.value;
        }
        
        Role role = roleRepository.findRoleById(roleId);
        if (role != null) {
            user.getRoles().add(role);
        }
        
        return userRepository.save(user);
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
        Optional<User> existingByGoogleId = userRepository.findByGoogleId(googleId);
        if(existingByGoogleId.isPresent())
            return;
        
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setGoogleId(googleId);
            user.setProvider(Provider.GOOGLE.value);
            if (user.getAvatar() == null) user.setAvatar(avatar);
            userRepository.save(user);
            return;
        }

        var user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode("OAUTH2_USER"));
        user.setProvider(Provider.GOOGLE.value);
        user.setGoogleId(googleId);
        user.setFullName(fullName);
        user.setAvatar(avatar);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String keyword, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase(Sort.Direction.ASC.name()) ? 
                    Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (keyword == null || keyword.isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchUsers(keyword, pageable);
    }

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

        user.getRoles().clear();

        roleNames.stream()
            .map(roleRepository::findByName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(user.getRoles()::add);

        return userRepository.save(user);
    }
    
    @Transactional
    public void assignRoleToUser(String username, String roleName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Long roleId = com.exam.examweb.constants.Role.student.value;
        if ("teacher".equalsIgnoreCase(roleName)) {
            roleId = com.exam.examweb.constants.Role.teacher.value;
        }

        Role role = roleRepository.findRoleById(roleId);
        if (role != null) {
            user.getRoles().add(role);
            userRepository.save(user);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateProfile(String username, User updatedUser) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        user.setFullName(updatedUser.getFullName());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        user.setDob(updatedUser.getDob());
        user.setAvatar(updatedUser.getAvatar());
        
        return userRepository.save(user);
    }
}
