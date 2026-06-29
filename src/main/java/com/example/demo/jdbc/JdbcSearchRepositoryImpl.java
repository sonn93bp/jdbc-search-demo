package com.example.demo.jdbc;

import com.example.demo.dto.BaseCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.BiFunction;

@Repository
@RequiredArgsConstructor
public class JdbcSearchRepositoryImpl implements JdbcSearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public <T> Page<T> search(SearchQuery<T, Void> query, Pageable pageable) {

        SearchContext<T> ctx = executeSearch(pageable, query);

        Long total = jdbc.queryForObject(
                SqlClause.safe(query.countSql().apply(ctx.whereSql())).sql(),
                copy(ctx.params()),
                Long.class
        );

        return new PageImpl<>(ctx.content(), pageable, total);
    }

    // =========================
    // ADVANCED SEARCH
    // =========================
    @Override
    public <R, T, Q extends BaseCount> R advanceSearch(
            Pageable pageable,
            SearchQuery<T, Q> query,
            BiFunction<Page<T>, Q, R> mapper) {

        SearchContext<T> ctx = executeSearch(pageable, query);

        Q count = jdbc.queryForObject(
                SqlClause.safe(query.countSql().apply(ctx.whereSql())).sql(),
                copy(ctx.params()),
                query.countMapper()
        );

        Page<T> page = new PageImpl<>(
                ctx.content(),
                pageable,
                count.getTotal()
        );

        return mapper.apply(page, count);
    }

    // =========================
    // CORE EXECUTION (FAST PATH)
    // =========================
    private <T, Q> SearchContext<T> executeSearch(
            Pageable pageable,
            SearchQuery<T, Q> query) {

        MapSqlParameterSource params = new MapSqlParameterSource();

        SqlQuery whereResult = query.where().build(params);

        String sql = query.root().apply(
                query.select().get(),
                whereResult.sql(),
                query.order().get(),
                PAGING_SQL
        );

        params.addValue("offset", pageable.getOffset());
        params.addValue("limit", pageable.getPageSize());

        List<T> content = jdbc.query(SqlClause.safe(sql).sql(), params, query.rowMapper());

        return new SearchContext<>(content, whereResult.sql(), params);
    }

    // =========================
    // IMMUTABLE CONTEXT
    // =========================
    private record SearchContext<T>(
            List<T> content,
            String whereSql,
            MapSqlParameterSource params
    ) {}

    // =========================
    // SAFE COPY PARAMS
    // =========================
    private MapSqlParameterSource copy(MapSqlParameterSource src) {
        return new MapSqlParameterSource(src.getValues());
    }

    // =========================
    // CONSTANT
    // =========================
    private static final String PAGING_SQL = """
        OFFSET :offset ROWS
        FETCH NEXT :limit ROWS ONLY
        """;

}
