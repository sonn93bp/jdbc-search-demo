package com.example.demo;

import com.example.demo.dto.SearchRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderDetail;
import com.example.demo.jdbc.OrderDetailRepository;
import com.example.demo.jdbc.OrderRepository;
import com.example.demo.jdbc.SearchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {


	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner init (SearchService searchService, OrderDetailRepository repository, OrderRepository orderRepository) {
		return  args -> {
			Order order = new Order();

			order.setRefNo("REF-001");
			order.setOrderNo("ORD-1001");
			order.setOrderDate(LocalDateTime.now());
			order.setOrderStatus("NEW");
			order.setShipDate(null);
			order  = orderRepository.save(order);
			OrderDetail detail = new OrderDetail();
			detail.setOrder(order);
			detail.setQuantity(2);
			detail.setCustomerName("Nguyen Van A");

			repository.save(detail);

			searchService.search(SearchRequest.builder()
					.from("2026-06-11")
					.to("2026-06-15")
					.selectFields(Map.of(
							"refNo", "o.ref_no",
							"orderNo", "o.order_no",
							"customerName", "odd.customer_name",
							"orderDate", "o.order_date",
							"status", "o.order_status"
					))
					.pageRequest(SearchRequest.PageRequest.builder()
							.page(0)
							.size(20)
							.sort("refNo")
							.order("ASC")
							.build())
					.build());
		};
	}
}
