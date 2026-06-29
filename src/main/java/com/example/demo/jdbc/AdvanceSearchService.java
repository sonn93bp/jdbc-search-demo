package com.example.demo.jdbc;

import com.example.demo.dto.AdvanceResult;
import com.example.demo.dto.CountRecord;
import com.example.demo.dto.OrderSearch;
import com.example.demo.dto.SearchRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
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
public class AdvanceSearchService {
    private final JdbcSearchRepository searchRepository;

    public AdvanceResult<OrderSearch> search(SearchRequest searchRequest) {
        JdbcSearchRepository.SearchQuery<OrderSearch, CountRecord> query = new JdbcSearchRepository.SearchQuery<>(
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
                COUNT(*) total,
                SUM(CASE WHEN o.order_status='SUCCESS' THEN 1 ELSE 0 END) totalSuccess,
                SUM(CASE WHEN o.order_status='FAILURE' THEN 1 ELSE 0 END) totalFailure
            FROM orders o
            WHERE %s
        """.formatted(where),
                new OrderMapper(),
                new CountMapper()
        );
        return searchRepository.advanceSearch(
                PageRequest.of(searchRequest.getPageRequest().getPage(), searchRequest.getPageRequest().getSize()),
                query,
                (page, countRecord) -> AdvanceResult.<OrderSearch>builder()
                        .page(page)
                        .totalFailure(countRecord.getTotalFailure())
                        .totalSuccess(countRecord.getTotalSuccess())
                        .build()
        );
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
        conditions.add(buildSubWhereClause(searchRequest, params));
        return new JdbcSearchRepository.SqlQuery(String.join(" AND ", conditions), params);
    }

    private String buildSubWhereClause(SearchRequest searchRequest, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        if (Strings.isNotBlank(searchRequest.getCustomerName())) {
            conditions.add("od.customer_name = :customerName");
            params.addValue("customerName", searchRequest.getCustomerName());
        }
        if (conditions.isEmpty()) {
            return "1 = 1";
        }
        return String.format("""
                EXISTS (
                SELECT 1 FROM order_detail od WHERE od.order_id = o.id
                    AND %s
                )
                """, String.join(" AND ", conditions));
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

    @Data
    public static class CountMapper implements RowMapper<CountRecord> {
        @Override
        public CountRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return CountRecord.builder()
                    .total(rs.getInt("total"))
                    .totalFailure(rs.getInt("totalFailure"))
                    .totalSuccess(rs.getInt("totalSuccess"))
                    .build();
        }
    }
}
