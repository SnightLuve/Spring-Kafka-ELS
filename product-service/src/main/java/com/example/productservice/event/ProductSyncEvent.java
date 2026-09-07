package com.example.productservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSyncEvent {

    private ActionType actionType;
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String brand;
    private Boolean active;

    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE
    }
}
