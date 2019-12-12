package com.march.coderobot;

import com.march.coderobot.util.Robot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CodeRobotApplicationTests {

    @Test
    void main() {
        String[] tables = new String[]{"t_org"};
        Robot.output(tables);
    }

}
