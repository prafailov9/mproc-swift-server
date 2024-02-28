    package com.ntros.mprocswift.dto;

import com.ntros.mprocswift.validation.OriginLessThanBound;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NonNull
@OriginLessThanBound
public class RangeRequest {

    @Min(1)
    private int origin;
    @Min(2)
    private int bound;

}
