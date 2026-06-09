package com.girinlog.auth.repository;

import com.girinlog.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(String githubId);

    boolean existsByGithubId(String githubId);
}
