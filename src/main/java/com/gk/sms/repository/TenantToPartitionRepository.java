package com.gk.sms.repository;

import com.gk.sms.entities.UserWiseKafkaPartition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantToPartitionRepository extends JpaRepository<UserWiseKafkaPartition,Long> {
    Optional<UserWiseKafkaPartition> findByUser_Id(String userId);
}
