package org.fffd.l23o6.service.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.fffd.l23o6.dao.OrderDao;
import org.fffd.l23o6.dao.RouteDao;
import org.fffd.l23o6.dao.TrainDao;
import org.fffd.l23o6.dao.UserDao;
import org.fffd.l23o6.pojo.entity.UserEntity;
import org.fffd.l23o6.pojo.enum_.OrderStatus;
import org.fffd.l23o6.exception.BizError;
import org.fffd.l23o6.pojo.entity.OrderEntity;
import org.fffd.l23o6.pojo.entity.RouteEntity;
import org.fffd.l23o6.pojo.entity.TrainEntity;
import org.fffd.l23o6.pojo.vo.order.OrderVO;
import org.fffd.l23o6.service.OrderService;
import org.fffd.l23o6.util.strategy.payment.AliPayStrategy;
import org.fffd.l23o6.util.strategy.payment.BalancePayStrategy;
import org.fffd.l23o6.util.strategy.train.GSeriesSeatStrategy;
import org.fffd.l23o6.util.strategy.train.KSeriesSeatStrategy;
import org.springframework.stereotype.Service;

import io.github.lyc8503.spring.starter.incantation.exception.BizException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderDao orderDao;
    private final UserDao userDao;
    private final TrainDao trainDao;
    private final RouteDao routeDao;

    public Long createOrder(String username, Long trainId, Long fromStationId, Long toStationId, String seatType,
            String discountStrategy, String payStrategy, Long seatNumber) {
        Long userId = userDao.findByUsername(username).getId();
        TrainEntity train = trainDao.findById(trainId).get();
        RouteEntity route = routeDao.findById(train.getRouteId()).get();
        int startStationIndex = route.getStationIds().indexOf(fromStationId);
        int endStationIndex = route.getStationIds().indexOf(toStationId);
        String seat = null;
        Long money = 0L;
        switch (train.getTrainType()) {
            case HIGH_SPEED:
                seat = GSeriesSeatStrategy.INSTANCE.allocSeat(startStationIndex, endStationIndex,
                        GSeriesSeatStrategy.GSeriesSeatType.fromString(seatType), train.getSeats());
                money = GSeriesSeatStrategy.INSTANCE.getMoney(seat);
                break;
            case NORMAL_SPEED:
                seat = KSeriesSeatStrategy.INSTANCE.allocSeat(startStationIndex, endStationIndex,
                        KSeriesSeatStrategy.KSeriesSeatType.fromString(seatType), train.getSeats());
                money = KSeriesSeatStrategy.INSTANCE.getMoney(seat);
                break;
        }
        if (seat == null) {
            throw new BizException(BizError.OUT_OF_SEAT);
        }
        OrderEntity order = OrderEntity.builder().trainId(trainId).userId(userId).seat(seat).discountStrategy(discountStrategy)
                .payStrategy(payStrategy).status(OrderStatus.PENDING_PAYMENT).arrivalStationId(toStationId).departureStationId(fromStationId)
                .money(money).usedMoney((double) 0).usedCredits(0L).isCreditsUpdated(false).build();
        train.setUpdatedAt(null);// force it to update
        trainDao.save(train);
        orderDao.save(order);
        return order.getId();
    }

    public List<OrderVO> listOrders(String username) {
        Long userId = userDao.findByUsername(username).getId();
        List<OrderEntity> orders = orderDao.findByUserId(userId);
        orders.sort((o1,o2)-> o2.getId().compareTo(o1.getId()));
        return orders.stream().map(order -> {
            TrainEntity train = trainDao.findById(order.getTrainId()).get();
            RouteEntity route = routeDao.findById(train.getRouteId()).get();
            int startIndex = route.getStationIds().indexOf(order.getDepartureStationId());
            int endIndex = route.getStationIds().indexOf(order.getArrivalStationId());
            checkOrderStatus(order.getId());
            return OrderVO.builder().id(order.getId()).trainId(order.getTrainId())
                    .seat(order.getSeat()).status(order.getStatus().getText())
                    .createdAt(order.getCreatedAt())
                    .startStationId(order.getDepartureStationId())
                    .endStationId(order.getArrivalStationId())
                    .departureTime(train.getDepartureTimes().get(startIndex))
                    .arrivalTime(train.getArrivalTimes().get(endIndex))
                    .money(order.getMoney())
                    .build();
        }).collect(Collectors.toList());
    }

    public OrderVO getOrder(Long id) {
        OrderEntity order = orderDao.findById(id).get();
        TrainEntity train = trainDao.findById(order.getTrainId()).get();
        RouteEntity route = routeDao.findById(train.getRouteId()).get();
        int startIndex = route.getStationIds().indexOf(order.getDepartureStationId());
        int endIndex = route.getStationIds().indexOf(order.getArrivalStationId());
//        checkOrderStatus(order.getId());
        return OrderVO.builder().id(order.getId()).trainId(order.getTrainId())
                .seat(order.getSeat()).status(order.getStatus().getText())
                .createdAt(order.getCreatedAt())
                .startStationId(order.getDepartureStationId())
                .endStationId(order.getArrivalStationId())
                .departureTime(train.getDepartureTimes().get(startIndex))
                .arrivalTime(train.getArrivalTimes().get(endIndex))
                .money(order.getMoney())
                .build();
    }

    public void cancelOrder(Long id) {
        OrderEntity order = orderDao.findById(id).get();

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException(BizError.ILLEAGAL_ORDER_STATUS);
        }

        // TODO: refund user's money and credits if needed
        // 当前为“未支付”，直接取消，当前为“已支付”，先退钱和积分再取消
        if (order.getStatus() == OrderStatus.PAID) {
            UserEntity user = userDao.findById(order.getUserId()).get();
            user.setBalance(user.getBalance() + order.getUsedMoney());
            user.setCredits(user.getCredits() + order.getUsedCredits());
        }

        // 进行订单取消，先归还座位，更新seatMap， 再设置订单状态为“已取消”
        TrainEntity train = trainDao.findById(order.getTrainId()).get();
        RouteEntity route = routeDao.findById(train.getRouteId()).get();
        int startIndex = route.getStationIds().indexOf(order.getDepartureStationId());
        int endIndex = route.getStationIds().indexOf(order.getArrivalStationId());
        switch (train.getTrainType()) {
            case HIGH_SPEED:
                GSeriesSeatStrategy.INSTANCE.updateSeatMap(trainDao, order.getSeat(), train, startIndex, endIndex);
                break;
            case NORMAL_SPEED:
                KSeriesSeatStrategy.INSTANCE.updateSeatMap(order.getSeat(), train.getSeats(), startIndex, endIndex);
                break;
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderDao.save(order);
        trainDao.save(train);
    }

    public void payOrder(Long id) {
        OrderEntity order = orderDao.findById(id).get();

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(BizError.ILLEAGAL_ORDER_STATUS);
        }

        // TODO: use payment strategy to pay!
        UserEntity user = userDao.findById(order.getUserId()).get();
        if (order.getPayStrategy().equals("余额支付"))
            BalancePayStrategy.INSTANCE.pay(order, user);
        else
            AliPayStrategy.INSTANCE.pay(order, user);

        // 支付完成后状态变为“已支付”, 保存一下订单和用户信息
        order.setStatus(OrderStatus.PAID);
        orderDao.save(order);
        userDao.save(user);

        // TODO: update user's credits, so that user can get discount next time
        checkOrderStatus(id);
    }

    public void checkOrderStatus(Long id) {
        OrderEntity order = orderDao.findById(id).get();
        TrainEntity train = trainDao.findById(order.getTrainId()).get();
        RouteEntity route = routeDao.findById(train.getRouteId()).get();
        int startIndex = route.getStationIds().indexOf(order.getDepartureStationId());
        Date departureTime = train.getDepartureTimes().get(startIndex);
        Date currentTime = new Date();

        // 计算两个时间之间的差值（以毫秒为单位）
        long differenceInMilliseconds = departureTime.getTime() - currentTime.getTime();
        // 将差值转换为分钟
        long differenceInMinutes = differenceInMilliseconds / (1000 * 60);

        // 当此时时间在发车前一小时之内, 订单状态变为已完成, 此时不可退款且客户获得本次订单的积分
        if (differenceInMinutes <= 60 && order.getStatus() == OrderStatus.PAID) {
            order.setStatus(OrderStatus.COMPLETED);
            // 只有不使用积分才能获得积分
            if ((order.getIsCreditsUpdated() == null || !order.getIsCreditsUpdated()) && order.getDiscountStrategy().equals("不使用积分")) {
                UserEntity user = userDao.findById(order.getUserId()).get();
                user.setCredits(user.getCredits() + 5 * order.getMoney());
                order.setIsCreditsUpdated(true);
                userDao.save(user);
            }
        }
        orderDao.save(order);
    }

}
