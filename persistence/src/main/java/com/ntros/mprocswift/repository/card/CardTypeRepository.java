package com.ntros.mprocswift.repository.card;

import com.ntros.mprocswift.model.card.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardTypeRepository extends JpaRepository<CardType, Integer> {

    Optional<CardType> findByType(@Param("type") String type);

}
