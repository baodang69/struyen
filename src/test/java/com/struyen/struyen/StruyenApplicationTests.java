package com.struyen.struyen;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires live PostgreSQL connection outside sandbox")
class StruyenApplicationTests {

	@Test
	void contextLoads() {
	}

}
