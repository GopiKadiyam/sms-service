package com.gk.sms.repository;

import com.gk.sms.entities.UserWiseServicePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWiseServiceTypeRepository extends JpaRepository<UserWiseServicePermissionEntity,Long> {
}
