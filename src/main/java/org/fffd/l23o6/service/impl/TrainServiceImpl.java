package org.fffd.l23o6.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.fffd.l23o6.dao.RouteDao;
import org.fffd.l23o6.dao.TrainDao;
import org.fffd.l23o6.mapper.TrainMapper;
import org.fffd.l23o6.pojo.entity.RouteEntity;
import org.fffd.l23o6.pojo.entity.TrainEntity;
import org.fffd.l23o6.pojo.enum_.TrainType;
import org.fffd.l23o6.pojo.vo.train.AdminTrainVO;
import org.fffd.l23o6.pojo.vo.train.TrainVO;
import org.fffd.l23o6.pojo.vo.train.TicketInfo;
import org.fffd.l23o6.pojo.vo.train.TrainDetailVO;
import org.fffd.l23o6.service.TrainService;
import org.fffd.l23o6.util.strategy.train.GSeriesSeatStrategy;
import org.fffd.l23o6.util.strategy.train.KSeriesSeatStrategy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.github.lyc8503.spring.starter.incantation.exception.BizException;
import io.github.lyc8503.spring.starter.incantation.exception.CommonErrorType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {
    private final TrainDao trainDao;
    private final RouteDao routeDao;

    @Override
    public TrainDetailVO getTrain(Long trainId) {
        TrainEntity train = trainDao.findById(trainId).get();
        RouteEntity route = routeDao.findById(train.getRouteId()).get();
        return TrainDetailVO.builder().id(trainId).date(train.getDate()).name(train.getName())
                .stationIds(route.getStationIds()).arrivalTimes(train.getArrivalTimes())
                .departureTimes(train.getDepartureTimes()).extraInfos(train.getExtraInfos()).build();
    }

    @Override
    public List<TrainVO> listTrains(Long startStationId, Long endStationId, String date) {
        // TODO
        // First, get all routes contains [startCity, endCity]
        List<RouteEntity> routeList = routeDao.findAll();
        List<Long> routeIds = new ArrayList<>();

        for (RouteEntity route : routeList) {
            List<Long> stationIds = route.getStationIds();
            if (stationIds.contains(startStationId) && stationIds.contains(endStationId)) {
                routeIds.add(route.getId());
            }
        }

        // Then, Get all trains on that day with the wanted routes
        List<TrainEntity> trainList = trainDao.findAll();
        List<TrainVO> trainVOList = new ArrayList<>();

        for (TrainEntity train : trainList) {
            if (routeIds.contains(train.getRouteId()) && train.getDate().equals(date)) {
                RouteEntity route = routeDao.findById(train.getRouteId()).get();
                List<Long> stationIds = route.getStationIds();
                int startIndex = stationIds.indexOf(startStationId);
                int endIndex = stationIds.indexOf(endStationId);

                List<TicketInfo> ticketInfoList = new ArrayList<>();
                switch (train.getTrainType()) {
                    case HIGH_SPEED:
                        Map<GSeriesSeatStrategy.GSeriesSeatType, Integer> GMap
                                = GSeriesSeatStrategy.INSTANCE.getLeftSeatCount(startIndex, endIndex, train.getSeats());
                        for (Map.Entry<GSeriesSeatStrategy.GSeriesSeatType, Integer> entry : GMap.entrySet()) {
                            GSeriesSeatStrategy.GSeriesSeatType seatType = entry.getKey();
                            TicketInfo ticketInfo = new TicketInfo(seatType.getText(), entry.getValue(),
                                    GSeriesSeatStrategy.INSTANCE.getPrice(seatType));
                            ticketInfoList.add(ticketInfo);
                        }
                        break;
                    case NORMAL_SPEED:
                        Map<KSeriesSeatStrategy.KSeriesSeatType, Integer> KMap
                                = KSeriesSeatStrategy.INSTANCE.getLeftSeatCount(startIndex, endIndex, train.getSeats());
                        for (Map.Entry<KSeriesSeatStrategy.KSeriesSeatType, Integer> entry : KMap.entrySet()) {
                            KSeriesSeatStrategy.KSeriesSeatType seatType = entry.getKey();
                            TicketInfo ticketInfo = new TicketInfo(seatType.getText(), entry.getValue(),
                                    KSeriesSeatStrategy.INSTANCE.getPrice(seatType));
                            ticketInfoList.add(ticketInfo);
                        }
                        break;
                }

                TrainVO trainVO = TrainVO.builder().id(train.getId()).name(train.getName())
                        .trainType(train.getTrainType().getText()).startStationId(startStationId)
                        .endStationId(endStationId).departureTime(train.getDepartureTimes().get(startIndex))
                        .arrivalTime(train.getArrivalTimes().get(endIndex)).ticketInfo(ticketInfoList).build();
                trainVOList.add(trainVO);
            }
        }

        return trainVOList;
    }

    @Override
    public List<AdminTrainVO> listTrainsAdmin() {
        return trainDao.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(TrainMapper.INSTANCE::toAdminTrainVO).collect(Collectors.toList());
    }

    @Override
    public void addTrain(String name, Long routeId, TrainType type, String date, List<Date> arrivalTimes,
            List<Date> departureTimes) {
        TrainEntity entity = TrainEntity.builder().name(name).routeId(routeId).trainType(type)
                .date(date).arrivalTimes(arrivalTimes).departureTimes(departureTimes).build();
        RouteEntity route = routeDao.findById(routeId).get();
        if (route.getStationIds().size() != entity.getArrivalTimes().size()
                || route.getStationIds().size() != entity.getDepartureTimes().size()) {
            throw new BizException(CommonErrorType.ILLEGAL_ARGUMENTS, "列表长度错误");
        }
        entity.setExtraInfos(new ArrayList<String>(Collections.nCopies(route.getStationIds().size(), "预计正点")));
        switch (entity.getTrainType()) {
            case HIGH_SPEED:
                entity.setSeats(GSeriesSeatStrategy.INSTANCE.initSeatMap(route.getStationIds().size()));
                break;
            case NORMAL_SPEED:
                entity.setSeats(KSeriesSeatStrategy.INSTANCE.initSeatMap(route.getStationIds().size()));
                break;
        }
        trainDao.save(entity);
    }

    @Override
    public void changeTrain(Long id, String name, Long routeId, TrainType type, String date, List<Date> arrivalTimes,
                            List<Date> departureTimes) {
        // TODO: edit train info, please refer to `addTrain` above
        TrainEntity train = trainDao.findById(id).get();
        RouteEntity route = routeDao.findById(routeId).get();

        if (route.getStationIds().size() != arrivalTimes.size()
                || route.getStationIds().size() != departureTimes.size()) {
            throw new BizException(CommonErrorType.ILLEGAL_ARGUMENTS, "列表长度错误");
        }

        train.setName(name);
        train.setRouteId(routeId);
        train.setTrainType(type);
        train.setDate(date);
        train.setArrivalTimes(arrivalTimes);
        train.setDepartureTimes(departureTimes);

        train.setExtraInfos(new ArrayList<String>(Collections.nCopies(route.getStationIds().size(), "预计正点")));
        switch (train.getTrainType()) {
            case HIGH_SPEED:
                train.setSeats(GSeriesSeatStrategy.INSTANCE.initSeatMap(route.getStationIds().size()));
                break;
            case NORMAL_SPEED:
                train.setSeats(KSeriesSeatStrategy.INSTANCE.initSeatMap(route.getStationIds().size()));
                break;
        }

        trainDao.save(train);
    }

    @Override
    public void deleteTrain(Long id) {
        trainDao.deleteById(id);
    }
}
