package com.merfonteen.userservice.repository;

import com.merfonteen.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndIsActiveTrue(String email);
    Optional<User> findByUsernameAndIsActiveTrue(String username);
}
