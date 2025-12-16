package ru.tsvetikov.warehouse.router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class WarehouseRouteOptimizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WarehouseRouteOptimizerApplication.class, args);
	}

}
