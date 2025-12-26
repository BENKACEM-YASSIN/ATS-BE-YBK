package com.ats.optimizer.repository;

import com.ats.optimizer.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<UserProfile, Long> {
    // We can fetch the latest profile
    UserProfile findTopByOrderByIdDesc();
}
