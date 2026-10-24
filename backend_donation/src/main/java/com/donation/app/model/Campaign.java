package com.donation.app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.donation.app.enums.CampaignStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "campaign")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "history", length = 255)
    private String history;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "image_id", referencedColumnName = "id")
    private Image image;

    @Column(name = "target_amount", precision = 10, scale = 2)
    private BigDecimal targetAmount;

    @Builder.Default
    @Column(name = "amount_collected", precision = 10, scale = 2)
    private BigDecimal amountCollected = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @jakarta.persistence.Transient
    @Builder.Default
    private Long donorsCount = 0L;

    public String getImage() {
        return (imageUrl != null && !imageUrl.isBlank())
                ? imageUrl
                : "https://images.unsplash.com/photo-1641736047654-48c2546fcf19?w=800&h=450&fit=crop&auto=format";
    }

    public String getCategory() {
        String text = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();
        if (text.contains("mesa") || text.contains("almuerzo") || text.contains("nutrici") || text.contains("aliment")) {
            return "Alimentación";
        }
        if (text.contains("agua") || text.contains("salud") || text.contains("médic") || text.contains("medic") || text.contains("sonrisa")) {
            return "Salud";
        }
        if (text.contains("techo") || text.contains("vivienda") || text.contains("hogar") || text.contains("casa")) {
            return "Vivienda";
        }
        return "Educación";
    }

    public String getBadgeClass() {
        return switch (getCategory()) {
            case "Salud" -> "badge-sal";
            case "Vivienda" -> "badge-viv";
            case "Alimentación" -> "badge-ali";
            default -> "badge-edu";
        };
    }

    public String getLocation() {
        String text = (description != null ? description : "") + " " + (history != null ? history : "");
        if (text.contains("Villa El Salvador")) return "Villa El Salvador, Lima";
        if (text.contains("Trujillo")) return "El Porvenir, Trujillo";
        if (text.contains("Piura") || text.contains("Sechura")) return "Sechura, Piura";
        if (text.contains("Cusco")) return "San Sebastián, Cusco";
        if (text.contains("Iquitos")) return "Belén, Iquitos";
        return "Lima, Perú";
    }

    public String getShortDesc() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return history != null ? history : "Proyecto social de Semillas de Esperanza.";
    }

    public BigDecimal getRaised() {
        return amountCollected != null ? amountCollected : BigDecimal.ZERO;
    }

    public BigDecimal getGoal() {
        return targetAmount != null ? targetAmount : BigDecimal.ZERO;
    }

    public int getPorcentaje() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal collected = getRaised();
        int pct = collected.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
        return Math.min(100, Math.max(0, pct));
    }

    public String getRaisedFormatted() {
        return formatCurrency(getRaised());
    }

    public String getGoalFormatted() {
        return formatCurrency(getGoal());
    }

    public long getDonors() {
        if (donorsCount != null && donorsCount > 0) {
            return donorsCount;
        }
        return getRaised().compareTo(BigDecimal.ZERO) > 0
                ? Math.max(1L, getRaised().divide(BigDecimal.valueOf(85), 0, java.math.RoundingMode.UP).longValue())
                : 0L;
    }

    private String formatCurrency(BigDecimal val) {
        if (val == null) return "S/0";
        if (val.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return String.format(java.util.Locale.US, "S/%.1fk", val.doubleValue() / 1000.0);
        }
        return String.format(java.util.Locale.US, "S/%.0f", val.doubleValue());
    }
}