package org.fffd.l23o6.util.strategy.payment;

import org.fffd.l23o6.pojo.entity.OrderEntity;
import org.fffd.l23o6.pojo.entity.UserEntity;

import java.util.List;

public abstract class PaymentStrategy {

    double[] powerTier = new double[]{0, 0.001, 0.0015, 0.002, 0.0025, 0.003};
    Long[] creditTier = new Long[]{0L, 1000L, 3000L, 10000L, 50000L};

    // TODO: implement this by adding necessary methods and implement specified strategy
    public void pay(OrderEntity orderEntity, UserEntity userEntity) {
        return;
    }
}
