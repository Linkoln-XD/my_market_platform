package com.market.order.order.mapper;

import com.market.order.order.domain.OrderEntity;
import com.market.order.order.domain.OrderItemEntity;
import com.market.order.order.dto.OrderItemResponse;
import com.market.order.order.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(OrderEntity entity);

    OrderItemResponse toItemResponse(OrderItemEntity entity);
}
