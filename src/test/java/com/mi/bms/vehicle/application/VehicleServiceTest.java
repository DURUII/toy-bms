package com.mi.bms.vehicle.application;

import com.mi.bms.shared.exceptions.BusinessException;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.application.impl.VehicleServiceImpl;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.domain.repository.VehicleRepository;
import com.mi.bms.vehicle.domain.service.VehicleDomainService;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleRequest;
import com.mi.bms.vehicle.interfaces.rest.dto.VehicleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleDomainService vehicleDomainService;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private BatteryType ternaryBatteryType;
    private Vehicle vehicle;
    private VehicleRequest vehicleRequest;

    @BeforeEach
    void setUp() {
        // Initialize test data
        ternaryBatteryType = BatteryType.builder()
                .id(1)
                .code("TERNARY")
                .name("三元电池")
                .build();

        // Create vehicle status
        Vehicle.VehicleStatus status = Vehicle.VehicleStatus.create(100L, 100);

        vehicle = Vehicle.create(
                "test123456789012",
                1,
                1,
                status);

        vehicleRequest = VehicleRequest.builder()
                .carId(1)
                .batteryTypeCode("TERNARY")
                .mileageKm(100L)
                .healthPct(100)
                .build();
    }

    @Test
    void createVehicle_Success() {
        // Given
        Vehicle.VehicleStatus status = Vehicle.VehicleStatus.create(
                vehicleRequest.getMileageKm(),
                vehicleRequest.getHealthPct());

        when(vehicleDomainService.createVehicle(
                vehicleRequest.getCarId(),
                vehicleRequest.getBatteryTypeCode(),
                status)).thenReturn(vehicle);

        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(vehicleDomainService.getBatteryTypeByCode(anyString())).thenReturn(ternaryBatteryType);

        // When
        VehicleResponse response = vehicleService.createVehicle(vehicleRequest);

        // Then
        assertNotNull(response);
        assertEquals(vehicle.getVid(), response.getVid());
        assertEquals(vehicle.getCarId(), response.getCarId());
        assertEquals(ternaryBatteryType.getCode(), response.getBatteryTypeCode());
        assertEquals(ternaryBatteryType.getName(), response.getBatteryTypeName());
        assertEquals(vehicle.getStatus().getMileageKm(), response.getMileageKm());
        assertEquals(vehicle.getStatus().getHealthPct(), response.getHealthPct());

        verify(vehicleDomainService).createVehicle(
                vehicleRequest.getCarId(),
                vehicleRequest.getBatteryTypeCode(),
                status);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_DuplicateCarId_ThrowsException() {
        // Given
        Vehicle.VehicleStatus status = Vehicle.VehicleStatus.create(
                vehicleRequest.getMileageKm(),
                vehicleRequest.getHealthPct());

        when(vehicleDomainService.createVehicle(
                vehicleRequest.getCarId(),
                vehicleRequest.getBatteryTypeCode(),
                status)).thenThrow(new BusinessException("ALREADY_EXISTS", "车架号已存在"));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> vehicleService.createVehicle(vehicleRequest));

        assertEquals("ALREADY_EXISTS", exception.getCode());
        assertTrue(exception.getMessage().contains("车架号已存在"));

        verify(vehicleDomainService).createVehicle(
                vehicleRequest.getCarId(),
                vehicleRequest.getBatteryTypeCode(),
                status);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void getVehicleById_Success() {
        // Given
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.of(vehicle));
        when(vehicleDomainService.getBatteryTypeByCode(anyString())).thenReturn(ternaryBatteryType);

        // When
        VehicleResponse response = vehicleService.getVehicleById(vehicle.getVid());

        // Then
        assertNotNull(response);
        assertEquals(vehicle.getVid(), response.getVid());
        assertEquals(vehicle.getCarId(), response.getCarId());
        assertEquals(ternaryBatteryType.getCode(), response.getBatteryTypeCode());

        verify(vehicleRepository).findById(vehicle.getVid());
    }

    @Test
    void getVehicleById_NotFound_ThrowsException() {
        // Given
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> vehicleService.getVehicleById("nonexistent"));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Vehicle"));
        assertTrue(exception.getMessage().contains("vid"));
        assertTrue(exception.getMessage().contains("nonexistent"));

        verify(vehicleRepository).findById("nonexistent");
        verify(vehicleDomainService, never()).getBatteryTypeByCode(anyString());
    }

    @Test
    void getVehicleByCarId_Success() {
        // Given
        when(vehicleDomainService.getVehicleByCarId(anyInt())).thenReturn(vehicle);
        when(vehicleDomainService.getBatteryTypeByCode(anyString())).thenReturn(ternaryBatteryType);

        // When
        VehicleResponse response = vehicleService.getVehicleByCarId(vehicle.getCarId());

        // Then
        assertNotNull(response);
        assertEquals(vehicle.getVid(), response.getVid());
        assertEquals(vehicle.getCarId(), response.getCarId());

        verify(vehicleDomainService).getVehicleByCarId(vehicle.getCarId());
    }

    @Test
    void getAllVehicles_Success() {
        // Given
        Vehicle.VehicleStatus status2 = Vehicle.VehicleStatus.create(200L, 95);
        Vehicle vehicle2 = Vehicle.create(
                "test987654321098",
                2,
                1,
                status2);

        when(vehicleRepository.findAll()).thenReturn(Arrays.asList(vehicle, vehicle2));
        when(vehicleDomainService.getBatteryTypeByCode(anyString())).thenReturn(ternaryBatteryType);

        // When
        List<VehicleResponse> responses = vehicleService.getAllVehicles();

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());

        VehicleResponse response1 = responses.get(0);
        assertEquals(vehicle.getVid(), response1.getVid());
        assertEquals(vehicle.getCarId(), response1.getCarId());

        VehicleResponse response2 = responses.get(1);
        assertEquals(vehicle2.getVid(), response2.getVid());
        assertEquals(vehicle2.getCarId(), response2.getCarId());

        verify(vehicleRepository).findAll();
    }
}