package com.example.demo.jdbc;

import com.example.demo.function.QuadFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = whereClause.apply(params);
        String select = selectClause.get();
        String order = orderClause.get();
        String paging = """
                OFFSET :offset ROWS
                FETCH NEXT :limit ROWS ONLY
                """;
        String sql = rootClause.apply(select, where, order, paging);

        params.addValue("offset", pageable.getPageNumber() * pageable.getPageSize());
        params.addValue("limit", pageable.getPageSize());

        List<T> content  = jdbc.query(sql, params, rowMapper);
        String countQuery = countClause.apply(where);
        Long total = jdbc.queryForObject(countQuery, params, Long.class);

        return new PageImpl<>(content, pageable, total);
    }

    public boolean isOracle() {
        try (Connection conn = jdbc.getJdbcTemplate()
                .getDataSource()
                .getConnection()) {

            String dbName = conn.getMetaData().getDatabaseProductName();
            return "Oracle".equalsIgnoreCase(dbName);
        } catch (SQLException e) {
            return false;
        }
    }
}
