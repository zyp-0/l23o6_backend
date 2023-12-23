package org.fffd.l23o6.util.strategy.train;

import java.util.*;

import jakarta.annotation.Nullable;
import org.fffd.l23o6.pojo.entity.TrainEntity;


public class KSeriesSeatStrategy extends TrainSeatStrategy {
    public static final KSeriesSeatStrategy INSTANCE = new KSeriesSeatStrategy();

    private final Map<Integer, String> SOFT_SLEEPER_SEAT_MAP = new HashMap<>();
    private final Map<Integer, String> HARD_SLEEPER_SEAT_MAP = new HashMap<>();
    private final Map<Integer, String> SOFT_SEAT_MAP = new HashMap<>();
    private final Map<Integer, String> HARD_SEAT_MAP = new HashMap<>();

    private final Map<KSeriesSeatType, Map<Integer, String>> TYPE_MAP = new HashMap<>() {{
        put(KSeriesSeatType.SOFT_SLEEPER_SEAT, SOFT_SLEEPER_SEAT_MAP);
        put(KSeriesSeatType.HARD_SLEEPER_SEAT, HARD_SLEEPER_SEAT_MAP);
        put(KSeriesSeatType.SOFT_SEAT, SOFT_SEAT_MAP);
        put(KSeriesSeatType.HARD_SEAT, HARD_SEAT_MAP);
    }};

    private final Map<KSeriesSeatType, Integer> PRICE_MAP = new HashMap<>() {{
        put(KSeriesSeatType.SOFT_SLEEPER_SEAT, 120);
        put(KSeriesSeatType.HARD_SLEEPER_SEAT, 80);
        put(KSeriesSeatType.SOFT_SEAT, 50);
        put(KSeriesSeatType.HARD_SEAT, 30);
    }};


    private KSeriesSeatStrategy() {

        int counter = 0;

        for (String s : Arrays.asList("软卧1号上铺", "软卧2号下铺", "软卧3号上铺", "软卧4号上铺", "软卧5号上铺", "软卧6号下铺", "软卧7号上铺", "软卧8号上铺")) {
            SOFT_SLEEPER_SEAT_MAP.put(counter++, s);
        }

        for (String s : Arrays.asList("硬卧1号上铺", "硬卧2号中铺", "硬卧3号下铺", "硬卧4号上铺", "硬卧5号中铺", "硬卧6号下铺", "硬卧7号上铺", "硬卧8号中铺", "硬卧9号下铺", "硬卧10号上铺", "硬卧11号中铺", "硬卧12号下铺")) {
            HARD_SLEEPER_SEAT_MAP.put(counter++, s);
        }

        for (String s : Arrays.asList("1车1座", "1车2座", "1车3座", "1车4座", "1车5座", "1车6座", "1车7座", "1车8座", "2车1座", "2车2座", "2车3座", "2车4座", "2车5座", "2车6座", "2车7座", "2车8座")) {
            SOFT_SEAT_MAP.put(counter++, s);
        }

        for (String s : Arrays.asList("3车1座", "3车2座", "3车3座", "3车4座", "3车5座", "3车6座", "3车7座", "3车8座", "3车9座", "3车10座", "4车1座", "4车2座", "4车3座", "4车4座", "4车5座", "4车6座", "4车7座", "4车8座", "4车9座", "4车10座")) {
            HARD_SEAT_MAP.put(counter++, s);
        }
    }

    public enum KSeriesSeatType implements SeatType {
        SOFT_SLEEPER_SEAT("软卧"), HARD_SLEEPER_SEAT("硬卧"), SOFT_SEAT("软座"), HARD_SEAT("硬座"), NO_SEAT("无座");
        private String text;
        KSeriesSeatType(String text){
            this.text=text;
        }
        public String getText() {
            return this.text;
        }
        public static KSeriesSeatType fromString(String text) {
            for (KSeriesSeatType b : KSeriesSeatType.values()) {
                if (b.text.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    // 根据座位类型获得票价，用于填充TicketInfo
    public int getPrice(KSeriesSeatType seatType) {
        return PRICE_MAP.get(seatType);
    }

    // 根据具体分配的座位获得订单金额，用于CreateOrder
    public Long getMoney(String seat) {
        List<String> list = Arrays.asList("软卧1号上铺", "软卧2号下铺", "软卧3号上铺", "软卧4号上铺", "软卧5号上铺", "软卧6号下铺", "软卧7号上铺", "软卧8号上铺");
        if (seat.charAt(0) == '1' || seat.charAt(0) == '2') {
            return 50L;
        } else if (seat.charAt(0) == '3' || seat.charAt(0) == '4') {
            return 30L;
        } else if (list.contains(seat)) {
            return 120L;
        } else {
            return 80L;
        }
    }

    public void updateSeatMap(String seat, boolean[][] seatMap, int startStationIndex, int endStationIndex) {
        endStationIndex -= 1;
        int flag = 0;
        int idx = 0;
        for (Map.Entry<Integer, String> entry : SOFT_SLEEPER_SEAT_MAP.entrySet()) {
            if (entry.getValue().equals(seat)) {
                idx = entry.getKey();
                flag = 1;
                break;
            }
        }
        if (flag == 0) {
            for (Map.Entry<Integer, String> entry : HARD_SLEEPER_SEAT_MAP.entrySet()) {
                if (entry.getValue().equals(seat)) {
                    idx = entry.getKey();
                    flag = 1;
                    break;
                }
            }
            if (flag == 0) {
                for (Map.Entry<Integer, String> entry : SOFT_SEAT_MAP.entrySet()) {
                    if (entry.getValue().equals(seat)) {
                        idx = entry.getKey();
                        flag = 1;
                        break;
                    }
                }
                if (flag == 0) {
                    for (Map.Entry<Integer, String> entry : HARD_SEAT_MAP.entrySet()) {
                        if (entry.getValue().equals(seat)) {
                            idx = entry.getKey();
                            flag = 1;
                            break;
                        }
                    }
                }
            }
        }
        for (int i = startStationIndex; i <= endStationIndex; i++) {
            seatMap[i][idx] = false; // false表示没人
        }
    }

    public @Nullable String allocSeat(int startStationIndex, int endStationIndex, KSeriesSeatType type, boolean[][] seatMap) {
        //endStationIndex - 1 = upper bound
        endStationIndex -= 1;
        int beginSeatIndex, finishSeatIndex;
        if (type == KSeriesSeatType.SOFT_SLEEPER_SEAT) {
            beginSeatIndex = 0;
            finishSeatIndex = SOFT_SLEEPER_SEAT_MAP.size() - 1;
        } else if (type == KSeriesSeatType.HARD_SLEEPER_SEAT) {
            beginSeatIndex = SOFT_SLEEPER_SEAT_MAP.size();
            finishSeatIndex = SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() - 1;
        } else if (type == KSeriesSeatType.SOFT_SEAT) {
            beginSeatIndex = SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size();
            finishSeatIndex = SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size() - 1;
        } else if (type == KSeriesSeatType.HARD_SEAT) {
            beginSeatIndex = SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size();
            finishSeatIndex = SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size() + HARD_SEAT_MAP.size() - 1;
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

    public Map<KSeriesSeatType, Integer> getLeftSeatCount(int startStationIndex, int endStationIndex, boolean[][] seatMap) {
        endStationIndex -= 1;
        Map<KSeriesSeatType, Integer> leftSeatCount = new HashMap<>();
        leftSeatCount.put(KSeriesSeatType.SOFT_SLEEPER_SEAT,
                calLeftSeatCount(0, SOFT_SLEEPER_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        leftSeatCount.put(KSeriesSeatType.HARD_SLEEPER_SEAT,
                calLeftSeatCount(SOFT_SLEEPER_SEAT_MAP.size(), SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        leftSeatCount.put(KSeriesSeatType.SOFT_SEAT,
                calLeftSeatCount(SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size(),
                        SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        leftSeatCount.put(KSeriesSeatType.HARD_SEAT,
                calLeftSeatCount(SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size(),
                        SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size() + HARD_SEAT_MAP.size() - 1,
                        startStationIndex, endStationIndex, seatMap));
        return leftSeatCount;
    }

    public boolean[][] initSeatMap(int stationCount) {
        return new boolean[stationCount - 1][SOFT_SLEEPER_SEAT_MAP.size() + HARD_SLEEPER_SEAT_MAP.size() + SOFT_SEAT_MAP.size() + HARD_SEAT_MAP.size()];
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
