package com.ntros.mprocswift.dto;

import com.ntros.mprocswift.validation.OriginLessThanBound;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@OriginLessThanBound
public class RangeRequest {

    @Min(1)
    private int origin;
    @Min(2)
    private int bound;

}
