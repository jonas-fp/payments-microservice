package com.jonasfp.paymentservice.ledger.infra;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.ledger.domain.JournalEntry;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
}
