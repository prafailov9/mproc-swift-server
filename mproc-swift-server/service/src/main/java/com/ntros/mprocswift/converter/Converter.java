package com.ntros.mprocswift.converter;

public interface Converter<D, M> {

    D toDTO(final M model);
    M toModel(final D dto);
}
