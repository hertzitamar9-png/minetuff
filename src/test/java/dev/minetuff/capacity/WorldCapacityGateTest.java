package dev.minetuff.capacity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class WorldCapacityGateTest {
    @Test
    void concurrentReservationsNeverExceedOneThousand() throws Exception {
        WorldCapacityGate gate = new WorldCapacityGate(1000);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        try {
            List<Callable<Boolean>> attempts = new ArrayList<>();
            for (int i = 0; i < 1500; i++) {
                UUID id = UUID.randomUUID();
                attempts.add(() -> gate.reserve(id, "mt_001_sakura-gorge_1"));
            }
            List<Future<Boolean>> futures = pool.invokeAll(attempts);
            long admitted = 0;
            for (Future<Boolean> future : futures) if (future.get()) admitted++;
            assertEquals(1000L, admitted);
            assertEquals(1000, gate.used("mt_001_sakura-gorge_1"));
            assertFalse(gate.reserve(UUID.randomUUID(), "mt_001_sakura-gorge_1"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void leavingWorldReopensCapacity() {
        WorldCapacityGate gate = new WorldCapacityGate(2);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertTrue(gate.reserve(a, "world"));
        assertTrue(gate.reserve(b, "world"));
        gate.enter(a, "world");
        gate.enter(b, "world");
        assertFalse(gate.reserve(c, "world"));
        gate.leave("world");
        assertTrue(gate.reserve(c, "world"));
    }
}
