package com.example.demo;

import com.example.demo.dto.SearchRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderDetail;
import com.example.demo.jdbc.AdvanceSearchService;
import com.example.demo.jdbc.OrderDetailRepository;
import com.example.demo.jdbc.OrderRepository;
import com.example.demo.jdbc.SearchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {


	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner init (AdvanceSearchService searchService, OrderDetailRepository repository, OrderRepository orderRepository) {
		return  args -> {

			Order order = new Order();
			order.setRefNo("REF-001");
			order.setOrderNo("ORD-1001");
			order.setOrderDate(LocalDateTime.now());
			order.setOrderStatus("SUCCESS");
			order.setShipDate(null);
			Order order2 = new Order();
			order2.setRefNo("REF-001");
			order2.setOrderNo("ORD-1001");
			order2.setOrderDate(LocalDateTime.now());
			order2.setOrderStatus("FAILURE");
			order2.setShipDate(null);
			order  = orderRepository.save(order);
			order2 = orderRepository.save(order2);

			OrderDetail detail = new OrderDetail();
			detail.setOrder(order);
			detail.setQuantity(2);
			detail.setCustomerName("Nguyen Van B1");

			OrderDetail detail1 = new OrderDetail();
			detail1.setOrder(order);
			detail1.setQuantity(1);
			detail1.setCustomerName("Nguyen Van A11");

			OrderDetail detail2 = new OrderDetail();
			detail2.setOrder(order2);
			detail2.setQuantity(2);
			detail2.setCustomerName("Nguyen Van B1");

			repository.saveAll(List.of(detail, detail2, detail1));

			var result = searchService.search(SearchRequest.builder()
					.from("2026-06-28")
					.to("2026-07-01")
					.selectFields(Map.of(
							"refNo", "o.ref_no",
							"orderNo", "o.order_no",
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

			System.out.println(result.getTotalFailure());
		};
	}
}
