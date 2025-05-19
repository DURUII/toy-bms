package com.mi.bms.rule.domain.repository;

import com.mi.bms.rule.domain.model.WarnRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarnRuleRepository extends JpaRepository<WarnRule, Long> {

    boolean existsByRuleNo(Integer ruleNo);

    @Query("SELECT r FROM WarnRule r WHERE r.ruleNo = :ruleNo AND r.batteryTypeId = :batteryTypeId AND r.isDelete = false")
    List<WarnRule> findByRuleNoAndBatteryTypeId(@Param("ruleNo") Integer ruleNo,
            @Param("batteryTypeId") Integer batteryTypeId);

    @Query("SELECT r FROM WarnRule r WHERE r.batteryTypeId = :batteryTypeId AND r.isDelete = false")
    List<WarnRule> findByBatteryTypeId(@Param("batteryTypeId") Integer batteryTypeId);

    List<WarnRule> findByRuleNo(Integer ruleNo);

    @Query("SELECT r FROM WarnRule r LEFT JOIN FETCH r.items WHERE r.id = ?1")
    Optional<WarnRule> findByIdWithItems(Long id);

    @Query("SELECT r FROM WarnRule r LEFT JOIN FETCH r.items WHERE r.ruleNo = ?1 AND r.batteryTypeId = ?2")
    List<WarnRule> findByRuleNoAndBatteryTypeIdWithItems(Integer ruleNo, Integer batteryTypeId);
}