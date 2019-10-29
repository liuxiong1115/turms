/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.common;

import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.DivideBy;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.BiFunction;

@Component
public class DateTimeUtil {
    private final TurmsClusterManager turmsClusterManager;

    public DateTimeUtil(TurmsClusterManager turmsClusterManager) {
        this.turmsClusterManager = turmsClusterManager;
    }

    public boolean checkRangesNumber(
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy,
            @Nullable Integer maxHourRanges,
            @Nullable Integer maxDayRanges,
            @Nullable Integer maxMonthRanges) {
        switch (divideBy) {
            case HOUR:
                if (maxHourRanges == null) {
                    return true;
                } else {
                    return getRangesNumber(startDate, endDate, divideBy) <= maxHourRanges;
                }
            case DAY:
                if (maxDayRanges == null) {
                    return true;
                } else {
                    return getRangesNumber(startDate, endDate, divideBy) <= maxDayRanges;
                }
            case MONTH:
                if (maxMonthRanges == null) {
                    return true;
                } else {
                    return getRangesNumber(startDate, endDate, divideBy) <= maxMonthRanges;
                }
            case NOOP:
            default:
                return true;
        }
    }

    public Integer getRangesNumber(
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy) {
        long differenceMillis = endDate.getTime() - startDate.getTime();
        switch (divideBy) {
            case HOUR:
                return (int) Math.ceil((double) differenceMillis / 3600000);
            case DAY:
                return (int) Math.ceil((double) differenceMillis / 86400000);
            case MONTH:
                return (int) Math.ceil((double) differenceMillis / 2629746000L);
            case NOOP:
            default:
                return 1;
        }
    }

    public List<Pair<Date, Date>> divide(
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy) {
        if (!endDate.after(startDate)) {
            return Collections.emptyList();
        } else {
            switch (divideBy) {
                case HOUR:
                    startDate = DateUtils.truncate(startDate, Calendar.HOUR);
                    endDate = DateUtils.truncate(endDate, Calendar.HOUR);
                    break;
                case DAY:
                    startDate = DateUtils.truncate(startDate, Calendar.DAY_OF_MONTH);
                    endDate = DateUtils.truncate(endDate, Calendar.DAY_OF_MONTH);
                    break;
                case MONTH:
                    startDate = DateUtils.truncate(startDate, Calendar.MONTH);
                    endDate = DateUtils.truncate(endDate, Calendar.MONTH);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + divideBy);
            }
            if (startDate.getTime() == endDate.getTime()) {
                return Collections.emptyList();
            } else {
                int unit;
                switch (divideBy) {
                    case HOUR:
                        unit = Calendar.HOUR_OF_DAY;
                        break;
                    case DAY:
                        unit = Calendar.DAY_OF_YEAR;
                        break;
                    case MONTH:
                        unit = Calendar.MONTH;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + divideBy);
                }
                List<Pair<Date, Date>> lists = new LinkedList<>();
                while (true) {
                    // Note: Do not use Instant because it doesn't support to plus months
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(startDate);
                    calendar.add(unit, 1);
                    Date currentEndDate = calendar.getTime();
                    if (currentEndDate.after(endDate)) {
                        break;
                    } else {
                        Pair<Date, Date> datePair = Pair.of(startDate, currentEndDate);
                        lists.add(datePair);
                        startDate = currentEndDate;
                    }
                }
                return lists;
            }
        }
    }

    //TODO: moves to somewhere more suitable
    public Mono<Pair<String, List<Map<String, ?>>>> queryBetweenDate(
            @NotNull String title,
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy,
            @NotNull Function3<Date, Date, ChatType, Mono<Long>> function,
            @Nullable ChatType chatType) {
        List<Pair<Date, Date>> dates = divide(startDate, endDate, divideBy);
        List<Mono<Map<String, ?>>> monos = new ArrayList<>(dates.size());
        for (Pair<Date, Date> datePair : dates) {
            Mono<Long> result = function.apply(
                    datePair.getLeft(),
                    datePair.getRight(),
                    chatType);
            monos.add(result.map(total -> Map.of("startDate", datePair.getLeft(),
                    "endDate", datePair.getRight(),
                    "total", total)));
        }
        return merge(title, monos);
    }

    public Mono<Pair<String, List<Map<String, ?>>>> queryBetweenDate(
            @NotNull String title,
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy,
            @NotNull BiFunction<Date, Date, Mono<Long>> function) {
        List<Pair<Date, Date>> dates = divide(startDate, endDate, divideBy);
        List<Mono<Map<String, ?>>> monos = new ArrayList<>(dates.size());
        for (Pair<Date, Date> datePair : dates) {
            Mono<Long> result = function.apply(
                    datePair.getLeft(),
                    datePair.getRight());
            monos.add(result.map(total -> Map.of("startDate", datePair.getLeft(),
                    "endDate", datePair.getRight(),
                    "total", total)));
        }
        return merge(title, monos);
    }

    public Mono<Pair<String, List<Map<String, ?>>>> checkAndQueryBetweenDate(
            @NotNull String title,
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy,
            @NotNull Function3<Date, Date, ChatType, Mono<Long>> function,
            @Nullable ChatType chatType) {
        int maxHourRanges = turmsClusterManager.getTurmsProperties()
                .getSecurity().getMaxHourRangesPerCountRequest();
        int maxDayRanges = turmsClusterManager.getTurmsProperties()
                .getSecurity().getMaxDayRangesPerCountRequest();
        int maxMonthRanges = turmsClusterManager.getTurmsProperties()
                .getSecurity().getMaxMonthRangesPerCountRequest();
        boolean checked = checkRangesNumber(startDate, endDate, divideBy,
                maxHourRanges, maxDayRanges, maxMonthRanges);
        if (checked) {
            return queryBetweenDate(title, startDate, endDate, divideBy, function, chatType);
        } else {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public Mono<Pair<String, List<Map<String, ?>>>> checkAndQueryBetweenDate(
            @NotNull String title,
            @NotNull Date startDate,
            @NotNull Date endDate,
            @NotNull DivideBy divideBy,
            @NotNull BiFunction<Date, Date, Mono<Long>> function) {
        int maxHourRanges = turmsClusterManager.getTurmsProperties()
                .getSecurity().getMaxHourRangesPerCountRequest();
        int maxDayRanges = turmsClusterManager.getTurmsProperties()
                .getSecurity().getMaxDayRangesPerCountRequest();
        int maxMonthRanges = turmsClusterManager.getTurmsProperties()
                .getSecurity().getMaxMonthRangesPerCountRequest();
        boolean checked = checkRangesNumber(startDate, endDate, divideBy,
                maxHourRanges, maxDayRanges, maxMonthRanges);
        if (checked) {
            return queryBetweenDate(title, startDate, endDate, divideBy, function);
        } else {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private Mono<Pair<String, List<Map<String, ?>>>> merge(@NotNull String title, List<Mono<Map<String, ?>>> monos) {
        Flux<Map<String, ?>> resultFlux = Flux.mergeOrdered((o1, o2) -> {
            Date startDate1 = (Date) o1.get("startDate");
            Date startDate2 = (Date) o2.get("startDate");
            if (startDate1.before(startDate2)) {
                return -1;
            } else if (startDate1.after(startDate2)) {
                return 1;
            }
            return 0;
        }, Flux.merge(monos));
        return resultFlux
                .collectList()
                .map(results -> Pair.of(title, results));
    }
}
