package com.kharcha.parsing.repository;

import com.kharcha.parsing.domain.UserMerchantCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMerchantCategoryRepository extends JpaRepository<UserMerchantCategory, UUID> {

    Optional<UserMerchantCategory> findByUserIdAndMerchantKey(UUID userId, String merchantKey);

    List<UserMerchantCategory> findByUserId(UUID userId);
}
