package com.mi.bms.rule.domain.model;

import com.mi.bms.shared.domain.ValueObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "warn_rule")
@Where(clause = "is_delete = 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For JPA
public class WarnRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    @Getter
    private Long id;

    @Column(name = "rule_no", nullable = false)
    @Getter
    private Integer ruleNo;

    @Column(name = "name", nullable = false, length = 64)
    @Getter
    private String name;

    @Column(name = "expr", nullable = false, length = 32)
    @Getter
    private String expr;

    @Column(name = "battery_type_id", nullable = false)
    @Getter
    private Integer batteryTypeId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter
    private LocalDateTime updatedAt;

    @Column(name = "is_delete")
    private Boolean isDelete = false;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleCondition> conditions = new ArrayList<>();

    // Factory method for creating new rules
    public static WarnRule create(Integer ruleNo, String name, String expr, Integer batteryTypeId) {
        WarnRule rule = new WarnRule();
        rule.ruleNo = ruleNo;
        rule.name = name;
        rule.expr = expr;
        rule.batteryTypeId = batteryTypeId;
        return rule;
    }

    // Encapsulated methods for managing conditions
    public void addCondition(RuleCondition condition) {
        validateCondition(condition);
        conditions.add(condition);
        condition.setRule(this);
    }

    public void removeCondition(RuleCondition condition) {
        conditions.remove(condition);
        condition.setRule(null);
    }

    public void clearConditions() {
        conditions.forEach(condition -> condition.setRule(null));
        conditions.clear();
    }

    public List<RuleCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    // Business logic methods
    public void updateBasicInfo(String name, String expr) {
        this.name = name;
        this.expr = expr;
    }

    public void markAsDeleted() {
        this.isDelete = true;
    }

    public boolean isDeleted() {
        return isDelete;
    }

    private void validateCondition(RuleCondition condition) {
        // Validate that condition ranges don't overlap with existing ones
        for (RuleCondition existing : conditions) {
            if (existing.overlapsWith(condition)) {
                throw new IllegalArgumentException("Condition ranges cannot overlap");
            }
        }
    }

    // Value object for rule conditions
    @Entity
    @Table(name = "warn_rule_item")
    @Where(clause = "is_delete = 0")
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class RuleCondition implements ValueObject {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "item_id")
        @Getter
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "rule_id", nullable = false)
        private WarnRule rule;

        @Column(name = "min_val")
        @Getter
        private BigDecimal minVal;

        @Column(name = "max_val")
        @Getter
        private BigDecimal maxVal;

        @Column(name = "warn_level", nullable = false)
        @Getter
        private Integer warnLevel;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        @Getter
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        @Getter
        private LocalDateTime updatedAt;

        @Column(name = "is_delete")
        private Boolean isDelete = false;

        public static RuleCondition create(BigDecimal minVal, BigDecimal maxVal, Integer warnLevel) {
            RuleCondition condition = new RuleCondition();
            condition.minVal = minVal;
            condition.maxVal = maxVal;
            condition.warnLevel = warnLevel;
            return condition;
        }

        public boolean isInRange(BigDecimal value) {
            if (minVal != null && value.compareTo(minVal) < 0) {
                return false;
            }
            if (maxVal != null && value.compareTo(maxVal) >= 0) {
                return false;
            }
            return true;
        }

        public boolean overlapsWith(RuleCondition other) {
            // If either range is unbounded, they don't overlap
            if (minVal == null || maxVal == null || other.minVal == null || other.maxVal == null) {
                return false;
            }

            // Check if ranges overlap
            return !(maxVal.compareTo(other.minVal) <= 0 || minVal.compareTo(other.maxVal) >= 0);
        }

        protected void setRule(WarnRule rule) {
            this.rule = rule;
        }
    }
}