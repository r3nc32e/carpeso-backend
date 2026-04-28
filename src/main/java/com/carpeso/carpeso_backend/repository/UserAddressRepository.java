package com.carpeso.carpeso_backend.repository;

import com.carpeso.carpeso_backend.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserId(Long userId);
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtAsc(Long userId);
}