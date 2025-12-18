package com.finsight.finsight.global.response;

public record DefaultIdResponse(
        Long id
) {
    public static DefaultIdResponse of(Long id) {
        return new DefaultIdResponse(id);
    }
}
