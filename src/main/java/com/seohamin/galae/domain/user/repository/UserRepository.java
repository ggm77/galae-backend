package com.seohamin.galae.domain.user.repository;

import com.seohamin.galae.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByHandle(String handle);

    boolean existsByHandle(String handle);

    List<User> findAllByHandleIn(Collection<String> handles);
}
