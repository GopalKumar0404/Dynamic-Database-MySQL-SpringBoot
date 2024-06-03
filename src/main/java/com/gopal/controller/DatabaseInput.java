package com.gopal.controller;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gopal.payload.database.DatabaseDetails;
import com.gopal.services.DynamicEntityService;

//@RestController
//@RequestMapping("/v1")
public class DatabaseInput {

	@Autowired
	private DynamicEntityService dynamicEntityService;

//	@PostMapping("/database/connect")
	public String connectToDatabase(@RequestBody DatabaseDetails details) {
		try {
			dynamicEntityService.setDataSource(details);
			return "Connected to the database!";
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return "Failed to connect to the database: " + e.getMessage();
		}
	}
}
