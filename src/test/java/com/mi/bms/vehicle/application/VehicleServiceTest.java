package com.mi.bms.vehicle.application;

import com.mi.bms.shared.exceptions.BusinessException;
import com.mi.bms.shared.exceptions.ResourceNotFoundException;
import com.mi.bms.vehicle.application.impl.VehicleServiceImpl;
import com.mi.bms.vehicle.domain.model.BatteryType;
import com.mi.bms.vehicle.domain.model.Vehicle;
import com.mi.bms.vehicle.domain.repository.BatteryTypeRepository;
import com.mi.bms.vehicle.domain.repository.VehicleRepository;
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
    private BatteryTypeRepository batteryTypeRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private BatteryType ternaryBatteryType;
    private Vehicle vehicle;
    private VehicleRequest vehicleRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        ternaryBatteryType = BatteryType.builder()
                .id(1)
                .code("TERNARY")
                .name("三元电池")
                .build();

        vehicle = Vehicle.builder()
                .vid("test123456789012")
                .carId(1)
                .batteryTypeId(1)
                .mileageKm(100L)
                .healthPct(100)
                .isDelete(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

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
        when(vehicleRepository.existsByCarId(anyInt())).thenReturn(false);
        when(batteryTypeRepository.findByCode(anyString())).thenReturn(Optional.of(ternaryBatteryType));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        // When
        VehicleResponse response = vehicleService.createVehicle(vehicleRequest);

        // Then
        assertNotNull(response);
        assertEquals(vehicle.getVid(), response.getVid());
        assertEquals(vehicle.getCarId(), response.getCarId());
        assertEquals(ternaryBatteryType.getCode(), response.getBatteryTypeCode());
        assertEquals(ternaryBatteryType.getName(), response.getBatteryTypeName());
        assertEquals(vehicle.getMileageKm(), response.getMileageKm());
        assertEquals(vehicle.getHealthPct(), response.getHealthPct());

        verify(vehicleRepository).existsByCarId(vehicleRequest.getCarId());
        verify(batteryTypeRepository).findByCode(vehicleRequest.getBatteryTypeCode());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_DuplicateCarId_ThrowsException() {
        // Given
        when(vehicleRepository.existsByCarId(anyInt())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> vehicleService.createVehicle(vehicleRequest));

        assertEquals("ALREADY_EXISTS", exception.getCode());
        assertTrue(exception.getMessage().contains("车架号已存在"));

        verify(vehicleRepository).existsByCarId(vehicleRequest.getCarId());
        verify(batteryTypeRepository, never()).findByCode(anyString());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_InvalidBatteryType_ThrowsException() {
        // Given
        when(vehicleRepository.existsByCarId(anyInt())).thenReturn(false);
        when(batteryTypeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> vehicleService.createVehicle(vehicleRequest));

        // 仅检查异常消息是否包含关键词
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("BatteryType"));
        assertTrue(exception.getMessage().contains("code"));
        assertTrue(exception.getMessage().contains(vehicleRequest.getBatteryTypeCode()));

        verify(vehicleRepository).existsByCarId(vehicleRequest.getCarId());
        verify(batteryTypeRepository).findByCode(vehicleRequest.getBatteryTypeCode());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void getVehicleById_Success() {
        // Given
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.of(vehicle));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When
        VehicleResponse response = vehicleService.getVehicleById(vehicle.getVid());

        // Then
        assertNotNull(response);
        assertEquals(vehicle.getVid(), response.getVid());
        assertEquals(vehicle.getCarId(), response.getCarId());
        assertEquals(ternaryBatteryType.getCode(), response.getBatteryTypeCode());

        verify(vehicleRepository).findById(vehicle.getVid());
        verify(batteryTypeRepository).findById(vehicle.getBatteryTypeId());
    }

    @Test
    void getVehicleById_NotFound_ThrowsException() {
        // Given
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> vehicleService.getVehicleById("nonexistent"));

        // 仅检查异常消息是否包含关键词
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Vehicle"));
        assertTrue(exception.getMessage().contains("vid"));
        assertTrue(exception.getMessage().contains("nonexistent"));

        verify(vehicleRepository).findById("nonexistent");
        verify(batteryTypeRepository, never()).findById(anyInt());
    }

    @Test
    void getVehicleByCarId_Success() {
        // Given
        when(vehicleRepository.findByCarId(anyInt())).thenReturn(Optional.of(vehicle));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

        // When
        VehicleResponse response = vehicleService.getVehicleByCarId(vehicle.getCarId());

        // Then
        assertNotNull(response);
        assertEquals(vehicle.getVid(), response.getVid());
        assertEquals(vehicle.getCarId(), response.getCarId());

        verify(vehicleRepository).findByCarId(vehicle.getCarId());
        verify(batteryTypeRepository).findById(vehicle.getBatteryTypeId());
    }

    @Test
    void getAllVehicles_Success() {
        // Given
        Vehicle vehicle2 = Vehicle.builder()
                .vid("test987654321098")
                .carId(2)
                .batteryTypeId(1)
                .mileageKm(200L)
                .healthPct(95)
                .build();

        when(vehicleRepository.findAll()).thenReturn(Arrays.asList(vehicle, vehicle2));
        when(batteryTypeRepository.findById(anyInt())).thenReturn(Optional.of(ternaryBatteryType));

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
        verify(batteryTypeRepository, times(2)).findById(anyInt());
    }
}