package com.mi.bms.warning.domain.repository;

import com.mi.bms.warning.domain.model.Warning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WarningRepository extends JpaRepository<Warning, Long> {

    @Query("SELECT w FROM Warning w WHERE w.carId = :carId AND w.createdAt BETWEEN :from AND :to AND w.isDelete = false")
    List<Warning> findByCarIdAndTimeRange(
            @Param("carId") Integer carId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT w FROM Warning w WHERE w.batteryTypeId = :batteryTypeId AND w.createdAt BETWEEN :from AND :to AND w.isDelete = false")
    List<Warning> findByBatteryTypeAndTimeRange(
            @Param("batteryTypeId") Integer batteryTypeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT w FROM Warning w WHERE w.carId = :carId AND w.batteryTypeId = :batteryTypeId AND w.createdAt BETWEEN :from AND :to AND w.isDelete = false")
    List<Warning> findByCarIdAndBatteryTypeAndTimeRange(
            @Param("carId") Integer carId,
            @Param("batteryTypeId") Integer batteryTypeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT w FROM Warning w WHERE w.createdAt BETWEEN :from AND :to AND w.isDelete = false")
    List<Warning> findByTimeRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}