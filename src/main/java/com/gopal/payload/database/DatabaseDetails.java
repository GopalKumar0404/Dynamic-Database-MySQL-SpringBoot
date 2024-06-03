package com.gopal.payload.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseDetails {
	private String jdbcUrl;
	private String username;
	private String password;
	private String driverClassName;

}
