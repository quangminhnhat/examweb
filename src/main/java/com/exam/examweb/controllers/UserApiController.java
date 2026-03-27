package com.exam.examweb.controllers;

import com.exam.examweb.entities.User;
import com.exam.examweb.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/management/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;

    // Enhanced Search API with Pagination and Sorting
    @GetMapping("/search")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("GET /api/management/users/search called with keyword: {}", keyword);
        return ResponseEntity.ok(userService.searchUsers(keyword, page, size, sortBy, direction));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("GET /api/management/users called");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("GET /api/management/users/{} called", id);
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        log.info("GET /api/management/users/by-username/{} called", username);
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<User> updateUserRoles(@PathVariable Long id, @RequestBody List<String> roleNames) {
        log.info("PUT /api/management/users/{}/roles called", id);
        try {
            return ResponseEntity.ok(userService.updateUserRoles(id, roleNames));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/management/users/{} called", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
