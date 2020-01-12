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

package im.turms.turms.pojo.domain;

import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@Document
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class UserLocation implements Serializable {
    @Id
    private Long id;

    @Indexed
    private Long userId;

    @Indexed
    private Float longitude;

    @Indexed
    private Float latitude;

    @Indexed
    private Date timestamp;

    private String name;

    private String address;

    @Transient
    private PointFloat point;

    public UserLocation(Long userId, Float longitude, Float latitude, Date timestamp) {
        this.userId = userId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
    }

    public PointFloat getPoint() {
        if (point == null) {
            point = PointFloat.create(this.longitude, this.latitude);
        }
        return point;
    }

    public PointFloat setPoint(@NotNull Float longitude, @NotNull Float latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        point = PointFloat.create(this.longitude, this.latitude);
        return point;
    }
}