package org.fffd.l23o6.util.strategy.train;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nullable;
import org.fffd.l23o6.dao.TrainDao;
import org.fffd.l23o6.pojo.entity.TrainEntity;


public class GSeriesSeatStrategy extends TrainSeatStrategy {
    public static final GSeriesSeatStrategy INSTANCE = new GSeriesSeatStrategy();

    private final Map<Integer, String> BUSINESS_SEAT_MAP = new HashMap<>();
    private final Map<Integer, String> FIRST_CLASS_SEAT_MAP = new HashMap<>();
    private final Map<Integer, String> SECOND_CLASS_SEAT_MAP = new HashMap<>();

    private final Map<GSeriesSeatType, Map<Integer, String>> TYPE_MAP = new HashMap<>() {{
        put(GSeriesSeatType.BUSINESS_SEAT, BUSINESS_SEAT_MAP);
        put(GSeriesSeatType.FIRST_CLASS_SEAT, FIRST_CLASS_SEAT_MAP);
        put(GSeriesSeatType.SECOND_CLASS_SEAT, SECOND_CLASS_SEAT_MAP);
    }};

    private final Map<GSeriesSeatType, Integer> PRICE_MAP = new HashMap<>() {{
        put(GSeriesSeatType.BUSINESS_SEAT, 200);
        put(GSeriesSeatType.FIRST_CLASS_SEAT, 120);
        put(GSeriesSeatType.SECOND_CLASS_SEAT, 80);
    }};


    private GSeriesSeatStrategy() {

        int counter = 0;

        for (String s : Arrays.asList("1车1A","1车1C","1车1F")) {
            BUSINESS_SEAT_MAP.put(counter++, s);
        }

        for (String s : Arrays.asList("2车1A","2车1C","2车1D","2车1F","2车2A","2车2C","2车2D","2车2F","3车1A","3车1C","3车1D","3车1F")) {
            FIRST_CLASS_SEAT_MAP.put(counter++, s);
        }

        for (String s : Arrays.asList("4车1A","4车1B","4车1C","4车1D","4车2F","4车2A","4车2B","4车2C","4车2D","4车2F","4车3A","4车3B","4车3C","4车3D","4车3F")) {
            SECOND_CLASS_SEAT_MAP.put(counter++, s);
        }
        
    }

    public enum GSeriesSeatType implements SeatType {
        BUSINESS_SEAT("商务座"), FIRST_CLASS_SEAT("一等座"), SECOND_CLASS_SEAT("二等座"), NO_SEAT("无座");
        private String text;
        GSeriesSeatType(String text){
            this.text=text;
        }
        public String getText() {
            return this.text;
        }
        public static GSeriesSeatType fromString(String text) {
            for (GSeriesSeatType b : GSeriesSeatType.values()) {
                if (b.text.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    // 根据座位类型获得票价，用于填充TicketInfo
    public int getPrice(GSeriesSeatType seatType) {
        return PRICE_MAP.get(seatType);
    }

    // 根据具体分配的座位获得订单金额，用于CreateOrder
    public Long getMoney(String seat) {
        if (seat.charAt(0) == '1') {
            return 200L;
        } else if (seat.charAt(0) == '4') {
            return 80L;
        } else {
            return 120L;
        }
    }

    public void updateSeatMap(TrainDao trainDao, String seat, TrainEntity train, int startStationIndex, int endStationIndex) {
        endStationIndex -= 1;
        boolean[][] seatMap = train.getSeats();

        int flag = 0;
        int idx = 0;
        for (Map.Entry<Integer, String> entry : BUSINESS_SEAT_MAP.entrySet()) {
            if (entry.getValue().equals(seat)) {
                idx = entry.getKey();
                flag = 1;
                break;
            }
        }
        if (flag == 0) {
            for (Map.Entry<Integer, String> entry : FIRST_CLASS_SEAT_MAP.entrySet()) {
                if (entry.getValue().equals(seat)) {
                    idx = entry.getKey();
                    flag = 1;
                    break;
                }
            }
            if (flag == 0) {
                for (Map.Entry<Integer, String> entry : SECOND_CLASS_SEAT_MAP.entrySet()) {
                    if (entry.getValue().equals(seat)) {
                        idx = entry.getKey();
                        flag = 1;
                        break;
                    }
                }
            }
        }

        for (int i = startStationIndex; i <= endStationIndex; i++) {
            seatMap[i][idx] = false; // false表示没人
        }
        train.setSeats(seatMap);
        trainDao.save(train);
    }

    public @Nullable String allocSeat(int startStationIndex, int endStationIndex, GSeriesSeatType type, boolean[][] seatMap) {
        //endStationIndex - 1 = upper bound
        endStationIndex -= 1;
        // TODO
        int beginSeatIndex, finishSeatIndex;
        if (type == GSeriesSeatType.BUSINESS_SEAT) {
            beginSeatIndex = 0;
            finishSeatIndex = BUSINESS_SEAT_MAP.size() - 1;
        } else if (type == GSeriesSeatType.FIRST_CLASS_SEAT) {
            beginSeatIndex = BUSINESS_SEAT_MAP.size();
            finishSeatIndex = BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size() - 1;
        } else if (type == GSeriesSeatType.SECOND_CLASS_SEAT) {
            beginSeatIndex = BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size();
            finishSeatIndex = BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size() + SECOND_CLASS_SEAT_MAP.size() - 1;
        } else {
            beginSeatIndex = 0;
            finishSeatIndex = -1;
        }

        Map<Integer, String> map = TYPE_MAP.get(type);
        for (int i = beginSeatIndex; i <= finishSeatIndex; i++) {
            boolean flag = false; // true表示有人了
            for (int j = startStationIndex; j <= endStationIndex; j++) {
                if (seatMap[j][i]) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                for (int j = startStationIndex; j <= endStationIndex; j++) {
                    seatMap[j][i] = true;
                }
                return map.get(i);
            }
        }
        return null;
    }

    public Map<GSeriesSeatType, Integer> getLeftSeatCount(int startStationIndex, int endStationIndex, boolean[][] seatMap) {
        // TODO
        endStationIndex -= 1;
        Map<GSeriesSeatType, Integer> leftSeatCount = new HashMap<>();
        leftSeatCount.put(GSeriesSeatType.BUSINESS_SEAT,
                calLeftSeatCount(0, BUSINESS_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        leftSeatCount.put(GSeriesSeatType.FIRST_CLASS_SEAT,
                calLeftSeatCount(BUSINESS_SEAT_MAP.size(), BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        leftSeatCount.put(GSeriesSeatType.SECOND_CLASS_SEAT,
                calLeftSeatCount(BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size(),
                        BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size() + SECOND_CLASS_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        return leftSeatCount;
    }

    public boolean[][] initSeatMap(int stationCount) {
        return new boolean[stationCount - 1][BUSINESS_SEAT_MAP.size() + FIRST_CLASS_SEAT_MAP.size() + SECOND_CLASS_SEAT_MAP.size()];
    }

    private int calLeftSeatCount(int beginSeatIndex, int finishSeatIndex, int startStationIndex, int endStationIndex, boolean[][] seatMap) {
        int count = 0;
        for (int i = beginSeatIndex; i <= finishSeatIndex; i++) {
            boolean flag = false; // true表示有人了
            for (int j = startStationIndex; j <= endStationIndex; j++) {
                if (seatMap[j][i]) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                count++;
            }
        }
        return count;
    }
}
