package com.grzechuhehe.SportsBettingManagerApp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=bezpiecznyKluczTestowyJWT1234567890bezSpacjiISpecjalnychZnakow",
    "app.jwtExpirationMs=604800000",
    "server.ssl.enabled=false",
    "DB_USERNAME=testuser",
    "DB_PASSWORD=testpassword"
})
class SportsBettingManagerAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
