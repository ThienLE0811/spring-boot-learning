package com.example.crudapi.repository;

import com.example.crudapi.entity.Role;
import com.example.crudapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Dung khi update: bo qua chinh ban ghi dang sua, neu khong no se tu bao trung voi chinh minh. */
    boolean existsByUsernameAndIdNot(String username, Long id);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /** Phuc vu rang buoc "he thong luon con it nhat mot ADMIN". */
    long countByRole(Role role);
}
