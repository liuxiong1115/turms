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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class QueryBuilder {
    public static Criteria EMPTY_CRITERIA = new Criteria();
    private List<Criteria> criteriaList;
    private Criteria finalCriteria;
    private Query outputQuery;

    private QueryBuilder() {
        criteriaList = new LinkedList<>();
    }

    public static QueryBuilder newBuilder() {
        return new QueryBuilder();
    }

    /**
     * [start, end)
     */
    public QueryBuilder addBetweenIfNotNull(
            @NotNull String key,
            @Nullable Object start,
            @Nullable Object end) {
        if (start != null && end == null) {
            criteriaList.add(Criteria.where(key).gte(start));
        } else if (start == null && end != null) {
            criteriaList.add(Criteria.where(key).lt(end));
        } else if (start != null) {
            criteriaList.add(Criteria.where(key).gte(start).lt(end));
        }
        return this;
    }

    public QueryBuilder add(Criteria criteria) {
        criteriaList.add(criteria);
        return this;
    }

    public QueryBuilder addIfNotNull(Criteria criteria, Object data) {
        if (data != null) {
            if (data instanceof Collection) {
                if (!((Collection) data).isEmpty()) {
                    criteriaList.add(criteria);
                }
            } else {
                criteriaList.add(criteria);
            }
        }
        return this;
    }

    public QueryBuilder max(String field) {
        if (outputQuery == null) {
            buildQuery();
        }
        this.outputQuery = outputQuery.limit(1)
                .with(Sort.by(Sort.Direction.DESC, field));
        return this;
    }

    public QueryBuilder min(String field) {
        if (outputQuery == null) {
            buildQuery();
        }
        this.outputQuery = outputQuery.limit(1)
                .with(Sort.by(Sort.Direction.ASC, field));
        return this;
    }

    public Query paginateIfNotNull(Integer page, Integer size) {
        if (page != null && size != null) {
            buildReadyCriteria();
            Query query;
            if (finalCriteria != null) {
                query = new Query(finalCriteria);
            } else {
                query = new Query();
            }
            return query.with(PageRequest.of(page, size));
        } else {
            return buildQuery();
        }
    }

    public Query paginateIfNotNull(Integer page, Integer size, @Nullable Sort.Direction direction) {
        if (direction == null) {
            return paginateIfNotNull(page, size);
        } else {
            if (page != null && size != null) {
                buildReadyCriteria();
                Query query;
                if (finalCriteria != null) {
                    query = new Query(finalCriteria);
                } else {
                    query = new Query();
                }
                return query.with(PageRequest.of(page, size, direction));
            } else {
                return buildQuery();
            }
        }
    }

    private void buildReadyCriteria() {
        Criteria criteria = buildCriteria();
        if (criteria != EMPTY_CRITERIA) {
            finalCriteria = criteria;
        }
    }

    public Criteria buildCriteria() {
        if (!criteriaList.isEmpty()) {
            Criteria criteria = new Criteria();
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
            return criteria;
        } else {
            return EMPTY_CRITERIA;
        }
    }

    public Query buildQuery() {
        if (outputQuery != null) {
            return outputQuery;
        } else {
            if (finalCriteria == null) {
                buildReadyCriteria();
            }
            if (finalCriteria != null) {
                outputQuery = new Query().addCriteria(finalCriteria);
                return outputQuery;
            } else {
                return new Query();
            }
        }
    }
}
