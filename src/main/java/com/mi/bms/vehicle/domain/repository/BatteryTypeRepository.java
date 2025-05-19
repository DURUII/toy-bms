package com.mi.bms.vehicle.domain.repository;

import com.mi.bms.vehicle.domain.model.BatteryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatteryTypeRepository extends JpaRepository<BatteryType, Integer> {

    Optional<BatteryType> findByCode(String code);

    boolean existsByCode(String code);
}