package com.example.demo.jdbc;

import com.example.demo.dto.BaseCount;
import com.example.demo.function.QuadFunction;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@NoRepositoryBean
public interface JdbcSearchRepository {

    <T> Page<T> search(SearchQuery<T, Void> query, Pageable pageable);

    <R, T, Q extends BaseCount> R advanceSearch(
            Pageable pageable,
            SearchQuery<T, Q> query,
            BiFunction<Page<T>, Q, R> mapper
    );

    @Builder
    record SqlQuery(
            String sql,
            MapSqlParameterSource params
    ) {}

    @Builder
    record SearchQuery<T, Q>(
            Supplier<String> select,
            Supplier<String> order,
            WhereClause where,
            QuadFunction<String, String, String, String, String> root,
            Function<String, String> countSql,
            RowMapper<T> rowMapper,
            RowMapper<Q> countMapper
    ) {}
}

@FunctionalInterface
interface WhereClause {
    JdbcSearchRepository.SqlQuery build(MapSqlParameterSource params);
}
