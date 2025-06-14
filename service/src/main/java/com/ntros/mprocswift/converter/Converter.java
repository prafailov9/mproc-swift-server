package com.ntros.mprocswift.converter;

public interface Converter<D, M> {

    D toDto(final M model);
    M toModel(final D dto);
}
