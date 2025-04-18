package com.gk.sms.repository;

import com.gk.sms.entities.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserAccountEntity,String> {
    Optional<UserAccountEntity> findByUsername(String userName);
}
