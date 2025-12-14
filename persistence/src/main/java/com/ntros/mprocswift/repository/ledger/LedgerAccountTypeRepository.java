package com.ntros.mprocswift.repository.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerAccountTypeRepository extends JpaRepository<LedgerAccountType, Integer> {
    Optional<LedgerAccountType> findByTypeCode(String typeCode);

}
