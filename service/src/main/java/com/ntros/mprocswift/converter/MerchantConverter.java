package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.MerchantDTO;
import com.ntros.mprocswift.model.Merchant;
import org.springframework.stereotype.Component;

@Component
public class MerchantConverter implements Converter<MerchantDTO, Merchant> {
    @Override
    public MerchantDTO toDTO(Merchant model) {
        MerchantDTO dto = new MerchantDTO();
        dto.setMerchantCategoryCode(model.getMerchantCategoryCode());
        dto.setMerchantName(model.getMerchantName());
        dto.setMerchantIdentifierCode(model.getMerchantIdentifierCode());
        dto.setMerchantContactDetails(model.getContactDetails());
        return dto;
    }

    @Override
    public Merchant toModel(MerchantDTO dto) {
        Merchant merchant = new Merchant();
        merchant.setContactDetails(dto.getMerchantContactDetails());
        merchant.setMerchantName(dto.getMerchantName());
        merchant.setMerchantCategoryCode(dto.getMerchantCategoryCode());
        merchant.setMerchantIdentifierCode(dto.getMerchantIdentifierCode());
        return merchant;
    }
}
