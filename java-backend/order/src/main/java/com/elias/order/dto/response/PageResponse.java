package com.elias.order.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
}
