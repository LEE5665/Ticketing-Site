package com.example.backend.seat.service;

public class SeatConflictException extends IllegalStateException {
    public SeatConflictException(String message) {
        super(message);
    }
}
