package com.mi.bms.rule.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "warn_rule")
@Where(clause = "is_delete = 0")
public class WarnRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "rule_no", nullable = false)
    private Integer ruleNo;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "expr", nullable = false, length = 32)
    private String expr;

    @Column(name = "battery_type_id", nullable = false)
    private Integer batteryTypeId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_delete")
    private Boolean isDelete = false;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarnRuleItem> items = new ArrayList<>();

    public void addItem(WarnRuleItem item) {
        items.add(item);
        item.setRule(this);
    }

    public void removeItem(WarnRuleItem item) {
        items.remove(item);
        item.setRule(null);
    }

    public void clearItems() {
        items.forEach(item -> item.setRule(null));
        items.clear();
    }
}