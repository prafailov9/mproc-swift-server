package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.dto.transfer.ExternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.ExternalTransferResponse;
import com.ntros.mprocswift.service.transfer.ExternalTransferService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfer/external")
public class ExternalTransferController extends AbstractTransferController<ExternalTransferRequest, ExternalTransferResponse> {
    protected ExternalTransferController(ExternalTransferService transferService) {
        super(transferService);
    }
}
