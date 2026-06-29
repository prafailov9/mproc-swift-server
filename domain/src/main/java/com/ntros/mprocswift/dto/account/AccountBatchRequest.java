package com.ntros.mprocswift.dto.account;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class AccountBatchRequest {
    private List<String> accountNumbers;

}
