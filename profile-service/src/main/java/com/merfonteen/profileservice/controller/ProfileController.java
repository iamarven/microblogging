package com.merfonteen.profileservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/profiles")
@RestController
public class ProfileController {

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserInfo(@PathVariable Long id) {

        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{id}/posts")
    public ResponseEntity<?> getUserPosts(@PathVariable Long id) {

        return ResponseEntity.ok().build();
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<?> getCommentsOnPost(@PathVariable Long id) {

        return ResponseEntity.ok().build();
    }
}
