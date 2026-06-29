package com.example.demo.jdbc;

import com.example.demo.dto.OrderSearch;
import com.example.demo.dto.SearchRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final JdbcSearchRepository searchRepository;

    public Page<OrderSearch> search(SearchRequest searchRequest) {
        JdbcSearchRepository.SearchQuery<OrderSearch, Void> query = new JdbcSearchRepository.SearchQuery<>(
                () -> buildSelectClause(searchRequest.getSelectFields()),
                () -> buildOrderClause(searchRequest.getPageRequest(), searchRequest.getSelectFields()),
                params -> buildWhereClause(searchRequest, params),
                (select, where, order, paging) -> """
            SELECT %s
            FROM orders o
            WHERE %s
            ORDER BY %s
            %s
        """.formatted(select, where, order, paging),
                where -> """
            SELECT
                COUNT(*) 
            FROM orders o
            WHERE %s
        """.formatted(where),
                new OrderMapper(),
                null
        );
        return searchRepository.search(query, PageRequest.of(searchRequest.getPageRequest().getPage(),
                searchRequest.getPageRequest().getSize()));
    }

    private JdbcSearchRepository.SqlQuery buildWhereClause(SearchRequest searchRequest, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        if (Strings.isNotBlank(searchRequest.getOrderNo())) {
            conditions.add("o.order_no = :orderNo");
            params.addValue("orderNo", searchRequest.getOrderNo());
        }
        if (Strings.isNotBlank(searchRequest.getFrom())) {
            conditions.add("o.order_date  >= :from ");
            params.addValue("from", LocalDate.parse(searchRequest.getFrom()).atStartOfDay());
        }
        if (Strings.isNotBlank(searchRequest.getTo())) {
            conditions.add("o.order_date  <= :to");
            params.addValue("to", LocalDate.parse(searchRequest.getTo()).atTime(LocalTime.MAX));
        }
        if (conditions.isEmpty()) {
            conditions.add("1 = 1");
        }
        return new JdbcSearchRepository.SqlQuery(String.join(" AND ", conditions), params);
    }

    private String buildSelectClause(Map<String, String> selectFields) {

        return selectFields.entrySet().stream()
                .map(entry -> setAliasForField(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", \n"));
    }


    private String buildOrderClause(SearchRequest.PageRequest pageRequest, Map<String, String> orderFields) {

        return String.join(" ", orderFields.getOrDefault(pageRequest.getSort(), "o.order_date"), pageRequest.getOrder());
    }

    private String setAliasForField(String key, String value) {
        return String.join(" AS ", value , key);
    }

    @Data
    public static class OrderMapper implements RowMapper<OrderSearch> {
        @Override
        public OrderSearch mapRow(ResultSet rs, int rowNum) throws SQLException {
            return OrderSearch.builder()
                    .orderNo(rs.getString("orderNo"))
                    .orderDate(rs.getTimestamp("orderDate").toLocalDateTime())
                    .status(rs.getString("status"))
                    .refNo(rs.getString("refNo"))
                    .build();
        }
    }
}
