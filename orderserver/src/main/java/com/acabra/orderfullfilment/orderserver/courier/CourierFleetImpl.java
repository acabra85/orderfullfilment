package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CourierFleetImpl implements CourierFleet {

    private static final Collector<Courier, ?, ArrayDeque<Courier>> DEQUE_COLLECTOR = Collectors.toCollection(ArrayDeque::new);

    private final Map<Integer, Courier> dispatchedCouriers;
    private final ConcurrentLinkedDeque<Courier> availableCouriers;

    private final AtomicInteger totalCouriers;
    private final AtomicReference<Deque<OutputEvent>> courierAvailableNotificationDeque;
    private final EtaEstimator etaEstimator;
    private final PriorityBlockingQueue<ScheduledArrival> scheduleDeque;
    private final ScheduledExecutorService dispatchExecutor;

    public CourierFleetImpl(List<Courier> couriers, EtaEstimator etaEstimator) {
        PriorityBlockingQueue<ScheduledArrival> dispatchDeque = new PriorityBlockingQueue<>();
        this.availableCouriers = new ConcurrentLinkedDeque<>(couriers.stream()
                .filter(Courier::isAvailable).collect(DEQUE_COLLECTOR));
        this.dispatchedCouriers = buildDispatchedMap(couriers);
        this.totalCouriers = new AtomicInteger(couriers.size());
        this.courierAvailableNotificationDeque = new AtomicReference<>();
        this.etaEstimator = etaEstimator;
        this.scheduleDeque = dispatchDeque;
        this.dispatchExecutor = startMonitor(dispatchDeque);
    }

    private Map<Integer, Courier> buildDispatchedMap(List<Courier> couriers) {
        return new ConcurrentHashMap<>(this.availableCouriers.size() != couriers.size() ?
                couriers.stream()
                        .filter(c -> !c.isAvailable())
                        .map(c-> Map.entry(c.id, c))
                        .collect(
                            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : new HashMap<>());
    }

    @Override
    public DispatchResult dispatch(DeliveryOrder order) {
        Courier courier = getAvailableCourier();
        dispatchedCouriers.put(courier.id, courier);
        int estimatedTravelTime = etaEstimator.estimateCourierTravelTimeInSeconds(courier);
        CompletableFuture<Boolean> schedule = this.schedule(estimatedTravelTime, courier.id);
        return DispatchResult.of(courier.id, schedule, estimatedTravelTime);
    }

    private Courier getAvailableCourier() {
        Courier poll = availableCouriers.poll();
        if(poll == null) {
            int id = this.totalCouriers.incrementAndGet();
            return Courier.ofAvailable(id, "CourierName:" + id);
        }
        return poll;
    }

    @Override
    public void release(Integer courierId) throws NoSuchElementException {
        Courier courier = this.dispatchedCouriers.get(courierId);
        if(null == courier) {
            String error = String.format("The given id [%d] does not correspond to an assigned courier", courierId);
            throw new NoSuchElementException(error);
        }
        courier.orderDelivered();
        this.dispatchedCouriers.remove(courierId);
        this.availableCouriers.add(courier);
        log.info("Courier[{},{}] is available ... remaining available couriers: {} ", courierId, courier.name, this.availableCouriers.size());
    }

    private CompletableFuture<Boolean> schedule(int timeToDestination, int courierId) {
        ScheduledArrival scheduledArrival = ScheduledArrival.of(courierId, timeToDestination * 1000L, this.courierAvailableNotificationDeque.get());
        scheduleDeque.offer(scheduledArrival);
        return scheduledArrival.completedNotification;
    }

    private static ScheduledExecutorService startMonitor(PriorityBlockingQueue<ScheduledArrival> scheduleDeque) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            long now = KitchenClock.now();
            if(!scheduleDeque.isEmpty() && scheduleDeque.peek().arrived(now)) {
                try {
                    ScheduledArrival poll = scheduleDeque.poll(0L, TimeUnit.MILLISECONDS);
                    if (null != poll) {
                        poll.apply(now);
                    }
                } catch (InterruptedException e) {
                    log.error("unable to retrieve from pq");
                }
            }
        }, 0, 900, TimeUnit.MILLISECONDS);
        return executorService;
    }

    @Override
    public int fleetSize() {
        return totalCouriers.get();
    }

    @Override
    public int availableCouriers() {
        return availableCouriers.size();
    }

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        courierAvailableNotificationDeque.set(deque);
    }

    @Override
    public void shutdown() {
        this.dispatchExecutor.shutdownNow();
    }

    private static class ScheduledArrival implements Function<Long, Void>, Comparable<ScheduledArrival> {
        private final Deque<OutputEvent> notificationQueue;
        public final int courierId;
        public final long eta;
        public final  CompletableFuture<Boolean> completedNotification;

        public ScheduledArrival(int courierId, long eta, Deque<OutputEvent> notificationQueue) {
            this.courierId = courierId;
            this.eta = KitchenClock.now() + eta;
            this.notificationQueue = notificationQueue;
            this.completedNotification = new CompletableFuture<>();
        }

        public static ScheduledArrival of(int courierId, long timeToDestination, Deque<OutputEvent> queue) {
            return new ScheduledArrival(courierId, timeToDestination, queue);
        }

        public boolean arrived(long now) {
            return now >= this.eta;
        }

        @Override
        public Void apply(Long now) {
            try {
                if(notificationQueue != null) {
                    notificationQueue.offer(CourierArrivedEvent.of(courierId, eta, now));
                    completedNotification.complete(true);
                    return null;
                } else{
                    completedNotification.complete(false);
                }
            } catch (Exception e) {
                String message = "Failed to publish the notification: " + e.getMessage();
                log.error(message, e);
                completedNotification.completeExceptionally(new Exception(message, e));
            }
            return null;
        }

        @Override
        public int compareTo(ScheduledArrival o) {
            return Long.compare(this.eta, o.eta);
        }
    }
}
