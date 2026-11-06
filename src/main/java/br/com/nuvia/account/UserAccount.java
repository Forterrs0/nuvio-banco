package br.com.nuvia.account;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_account_phone", columnNames = "phone")
})
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String fullName;
    @Column(nullable = false, length = 180)
    private String email;
    @Column(nullable = false, length = 18)
    private String phone;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private long balanceCents;
    @Version
    private long version;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {}
    public UserAccount(String fullName, String email, String phone, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public long getBalanceCents() { return balanceCents; }
    public Instant getCreatedAt() { return createdAt; }
    public void credit(long cents) { this.balanceCents = Math.addExact(this.balanceCents, cents); }
    public void debit(long cents) {
        if (cents > balanceCents) throw new IllegalArgumentException("Saldo insuficiente para essa transferência.");
        this.balanceCents = Math.subtractExact(this.balanceCents, cents);
    }
}
