package br.com.nuvia.transfer;

import br.com.nuvia.account.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ledger_public_id", columnNames = "public_id"),
    @UniqueConstraint(name = "uk_ledger_idempotency", columnNames = {"initiator_id", "idempotency_key"})
}, indexes = {
    @Index(name = "idx_ledger_created", columnList = "created_at")
})
public class LedgerEntry {
    public enum Type { DEMO_DEPOSIT, INTERNAL_PIX }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private String publicId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Type type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private UserAccount sender;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;
    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;
    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;
    @Column(nullable = false)
    private long amountCents;
    @Column(nullable = false, length = 140)
    private String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {}
    public LedgerEntry(Type type, UserAccount sender, UserAccount recipient, long amountCents, String description, String idempotencyKey, Long initiatorId) {
        this.publicId = UUID.randomUUID().toString();
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.amountCents = amountCents;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.initiatorId = initiatorId;
        this.createdAt = Instant.now();
    }
    public String getPublicId() { return publicId; }
    public Type getType() { return type; }
    public UserAccount getSender() { return sender; }
    public UserAccount getRecipient() { return recipient; }
    public Long getInitiatorId() { return initiatorId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getAmountCents() { return amountCents; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
