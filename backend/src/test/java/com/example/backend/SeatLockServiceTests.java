package com.example.backend;

import com.example.backend.seat.service.SeatLockService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeatLockServiceTests {
    @Test
    void overlappingSelectionReleasesPartialLocksWithoutEnteringDatabase() {
        RedissonClient client = mock(RedissonClient.class);
        RLock first = mock(RLock.class);
        RLock second = mock(RLock.class);
        when(client.getLock("seat:lock:1:A1")).thenReturn(first);
        when(client.getLock("seat:lock:1:A2")).thenReturn(second);
        when(first.tryLock()).thenReturn(true);
        when(second.tryLock()).thenReturn(false);
        AtomicBoolean entered = new AtomicBoolean();

        assertThatThrownBy(() -> new SeatLockService(client).withLocks(1L, List.of("A2", "A1", "A1"), () -> {
            entered.set(true);
            return null;
        })).isInstanceOf(IllegalStateException.class);

        assertThat(entered).isFalse();
        verify(first).tryLock();
        verify(first).unlock();
        verify(second, never()).unlock();
    }

    @Test
    void databaseFailureStillReleasesAllLocks() {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("seat:lock:1:A1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        RuntimeException failure = new RuntimeException("transaction failed");
        assertThatThrownBy(() -> new SeatLockService(client).withLocks(1L, List.of("A1"), () -> {
            throw failure;
        })).isSameAs(failure);
        verify(lock).unlock();
    }
}
