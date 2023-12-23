package org.fffd.l23o6.util.strategy.payment;

import org.fffd.l23o6.pojo.entity.OrderEntity;
import org.fffd.l23o6.pojo.entity.UserEntity;

public class BalancePayStrategy extends PaymentStrategy{
    public static final BalancePayStrategy INSTANCE = new BalancePayStrategy();

    @Override
    public void pay(OrderEntity orderEntity, UserEntity userEntity) {
        if (orderEntity.getDiscountStrategy().equals("不使用积分")) {
            userEntity.setBalance(userEntity.getBalance() - orderEntity.getMoney());
            // 保存相关信息用于取消订单
            orderEntity.setUsedMoney((double) orderEntity.getMoney());
            orderEntity.setUsedCredits(0L);
        } else {
            Long originalCredits = userEntity.getCredits();
            Long credits = userEntity.getCredits();
            Long money = orderEntity.getMoney();
            double discountMoney = 0;
            double remainedMoney = money;
            // 调整积分
            if (credits > 50000) {
                discountMoney = (credits - 50000) * 0.003;
                if (discountMoney > money) {
                    userEntity.setCredits(credits - Math.round(money / 0.003));
                    remainedMoney = 0;
                } else {
                    credits = 50000L;
                    remainedMoney -= discountMoney;
                    for (int i = 4; i >= 1; i--) {
                        discountMoney += powerTier[i] * (creditTier[i] - creditTier[i - 1]);
                        if (discountMoney > remainedMoney) {
                            remainedMoney = 0;
                            userEntity.setCredits(credits - Math.round(remainedMoney / powerTier[i]));
                            break;
                        } else {
                            credits = creditTier[i - 1];
                            remainedMoney -= discountMoney;
                        }
                    }
                    if (credits == 0L) {
                        userEntity.setCredits(credits);
                    }
                }
            } else if (credits <= 50000 && credits >= 1000){
                int idx = 4;
                for (int i = 4; i >= 2; i--) {
                    if (creditTier[i] >= credits && credits >= creditTier[i - 1]) {
                        idx = i;
                        break;
                    }
                }
                for (int i = idx; i >= 1; i--) {
                    discountMoney += powerTier[i] * (creditTier[i] - creditTier[i - 1]);
                    if (discountMoney > remainedMoney) {
                        remainedMoney = 0;
                        userEntity.setCredits(credits - Math.round(remainedMoney / powerTier[i]));
                        break;
                    } else {
                        credits = creditTier[i - 1];
                        remainedMoney -= discountMoney;
                    }
                }
                if (credits == 0L) {
                    userEntity.setCredits(credits);
                }
            } else {
                // 积分不足以使用抵扣
            }
            // 付款
            userEntity.setBalance(userEntity.getBalance() - remainedMoney);

            // 保存相关信息用于取消订单
            orderEntity.setUsedMoney(remainedMoney);
            orderEntity.setUsedCredits(originalCredits - userEntity.getCredits());
        }
    }
}
