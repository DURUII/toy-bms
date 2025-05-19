package com.mi.bms.vehicle.domain.repository;

import com.mi.bms.vehicle.domain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    Optional<Vehicle> findByCarId(Integer carId);

    boolean existsByCarId(Integer carId);
}