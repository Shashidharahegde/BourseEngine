package com.bourse.dto;

public record OrderRequest( 
    String symbol,
    String side,
    String type,
    Long price,
    Long quantity 
){}
