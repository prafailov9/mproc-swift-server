package com.ntros.mprocswift.repository;

import com.ntros.mprocswift.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    Optional<Merchant> findByMerchantName(String merchantName);

}
