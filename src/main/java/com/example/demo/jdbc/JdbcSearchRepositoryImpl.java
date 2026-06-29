package com.example.demo.jdbc;

import com.example.demo.dto.BaseCount;
import com.example.demo.function.QuadFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Repository
@RequiredArgsConstructor
public class JdbcSearchRepositoryImpl implements JdbcSearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public <T> Page<T> search(Supplier<String> selectClause,
                          Supplier<String> orderClause,
                          Function<MapSqlParameterSource, String> whereClause,
                          QuadFunction<String, String, String, String, String> rootClause,
                          Function<String, String> countClause,
                          RowMapper<T> rowMapper,
                          Pageable pageable) {
        SearchContext<T> ctx = executeSearch(
                selectClause,
                orderClause,
                whereClause,
                rootClause,
                rowMapper,
                pageable);

        Long total = jdbc.queryForObject(
                countClause.apply(ctx.where()),
                ctx.params(),
                Long.class);

        return new PageImpl<>(ctx.content(), pageable, total);
    }

    @Override
    public <R, T , Q extends BaseCount> R advanceSearch(Supplier<String> selectClause, Supplier<String> orderClause,
                                                        Function<MapSqlParameterSource, String> whereClause,
                                                        QuadFunction<String, String, String, String, String> rootClause,
                                                        Function<String, String> countClause,
                                                        RowMapper<T> rowMapper,
                                                        RowMapper<Q> countMapper,
                                                        BiFunction<Page<T>, Q, R> mapResult,
                                                        Pageable pageable) {
        SearchContext<T> ctx = executeSearch(
                selectClause,
                orderClause,
                whereClause,
                rootClause,
                rowMapper,
                pageable);

        Q count = jdbc.queryForObject(
                countClause.apply(ctx.where()),
                ctx.params(),
                countMapper);

        Page<T> page = new PageImpl<>(
                ctx.content(),
                pageable,
                count.getTotal());

        return mapResult.apply(page, count);
    }

    private <T> SearchContext<T> executeSearch(
            Supplier<String> selectClause,
            Supplier<String> orderClause,
            Function<MapSqlParameterSource, String> whereClause,
            QuadFunction<String, String, String, String, String> rootClause,
            RowMapper<T> rowMapper,
            Pageable pageable) {

        MapSqlParameterSource params = new MapSqlParameterSource();

        String where = whereClause.apply(params);
        String select = selectClause.get();
        String order = orderClause.get();

        String paging = """
            OFFSET :offset ROWS
            FETCH NEXT :limit ROWS ONLY
            """;

        String sql = rootClause.apply(select, where, order, paging);

        params.addValue("offset", pageable.getOffset());
        params.addValue("limit", pageable.getPageSize());

        List<T> content = jdbc.query(sql, params, rowMapper);

        return new SearchContext<>(content, params, where);
    }

    private record SearchContext<T>(
            List<T> content,
            MapSqlParameterSource params,
            String where
    ) {}

}
