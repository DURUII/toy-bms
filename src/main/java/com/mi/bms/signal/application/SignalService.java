package com.mi.bms.signal.application;

import com.mi.bms.signal.interfaces.rest.dto.SignalRequest;
import com.mi.bms.signal.interfaces.rest.dto.SignalResponse;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

public interface SignalService {

    /**
     * 上报电池信号
     */
    List<SignalResponse> reportSignals(@Valid List<SignalRequest> requests);

    /**
     * 查询指定车辆的信号
     */
    List<SignalResponse> getSignalsByCarId(Integer carId, LocalDateTime from, LocalDateTime to);

    /**
     * 查询所有信号
     */
    List<SignalResponse> getAllSignals(LocalDateTime from, LocalDateTime to);
} 