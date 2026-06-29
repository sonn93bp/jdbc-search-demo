package com.example.demo.jdbc;

import com.example.demo.dto.BaseCount;
import com.example.demo.function.QuadFunction;
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

    <T> Page<T> search(Supplier<String> selectClause,
                          Supplier<String> orderClause,
                          Function<MapSqlParameterSource, String> whereClause,
                          QuadFunction<String, String, String, String, String> rootClause,
                          Function<String, String> countClause,
                          RowMapper<T> rowMapper,
                          Pageable pageable);

    <R, T , Q extends BaseCount> R advanceSearch(Supplier<String> selectClause,
                                                 Supplier<String> orderClause,
                                                 Function<MapSqlParameterSource, String> whereClause,
                                                 QuadFunction<String, String, String, String, String> rootClause,
                                                 Function<String, String> countClause,
                                                 RowMapper<T> rowMapper,
                                                 RowMapper<Q> countMapper,
                                                 BiFunction<Page<T>, Q , R> mapResult,
                                                 Pageable pageable);
}
