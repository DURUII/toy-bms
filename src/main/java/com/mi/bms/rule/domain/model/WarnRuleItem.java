package com.mi.bms.rule.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "warn_rule_item")
@Where(clause = "is_delete = 0")
public class WarnRuleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private WarnRule rule;

    @Column(name = "min_val")
    private BigDecimal minVal;

    @Column(name = "max_val")
    private BigDecimal maxVal;

    @Column(name = "warn_level", nullable = false)
    private Integer warnLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_delete")
    private Boolean isDelete = false;

    /**
     * 判断数值是否在该区间内
     */
    public boolean isInRange(BigDecimal value) {
        // 最小值检查（如果minVal为null则表示无下限）
        if (minVal != null && value.compareTo(minVal) < 0) {
            return false;
        }

        // 最大值检查（如果maxVal为null则表示无上限）
        if (maxVal != null && value.compareTo(maxVal) >= 0) {
            return false;
        }

        return true;
    }
}