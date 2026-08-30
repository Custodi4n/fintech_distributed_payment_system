package com.fintech.payment.repository;

import com.fintech.payment.domain.OutboxEvent;
import com.fintech.payment.domain.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}