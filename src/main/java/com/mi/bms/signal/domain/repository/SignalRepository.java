package com.mi.bms.signal.domain.repository;

import com.mi.bms.signal.domain.model.Signal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SignalRepository extends JpaRepository<Signal, Long> {

    @Query("SELECT s FROM Signal s WHERE s.carId = :carId AND s.processed = false AND s.isDelete = false")
    List<Signal> findUnprocessedSignalsByCarId(@Param("carId") Integer carId);

    @Query("SELECT s FROM Signal s WHERE s.processed = false AND s.isDelete = false")
    List<Signal> findAllUnprocessedSignals();

    @Query("SELECT s FROM Signal s WHERE s.carId = :carId AND s.createdAt BETWEEN :from AND :to AND s.isDelete = false")
    List<Signal> findByCarIdAndTimeRange(
            @Param("carId") Integer carId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT s FROM Signal s WHERE s.createdAt BETWEEN :from AND :to AND s.isDelete = false")
    List<Signal> findByTimeRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}