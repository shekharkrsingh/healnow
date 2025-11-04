package com.heal.doctor.dto;

import com.heal.doctor.dto.WebSocketResponseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebsocketResponseDTO <T>{
    private WebSocketResponseType type;
    private T payload;

    public static <T> WebsocketResponseDTO.WebsocketResponseDTOBuilder<T> builderGeneric() {
        return new WebsocketResponseDTO.WebsocketResponseDTOBuilder<>();
    }
}