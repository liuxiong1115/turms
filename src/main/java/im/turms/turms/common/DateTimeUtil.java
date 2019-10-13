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

import lombok.Getter;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DateTimeUtil {
    @Getter
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Getter
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    @Getter
    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

    public static Date endOfDay(Date day) {
        day = DateUtils.truncate(day, Calendar.DAY_OF_MONTH);
        return new Date(day.getTime() + 86399000);
    }

    public static Date parseDay(String day) throws ParseException {
        if (day != null) {
            return DateUtils.parseDate(day, Locale.getDefault(), "yyyy-MM-dd");
        } else {
            return null;
        }
    }

    public static Date endOfDay(String day) throws ParseException {
        if (day != null) {
            return endOfDay(parseDay(day));
        } else {
            return null;
        }
    }

    public static String now() {
        return LocalDateTime.now().format(dateTimeFormatter);
    }

    public static List<String> getDatesBetweenTwoDates(String startDate, String endDate, boolean included) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<String> totalDates = new LinkedList<>();
        if (included) {
            totalDates.add(startDate);
        }
        while (!start.isAfter(end)) {
            totalDates.add(start.toString());
            start = start.plusDays(1);
        }
        if (included) {
            totalDates.add(endDate);
        }
        return totalDates;
    }
}
