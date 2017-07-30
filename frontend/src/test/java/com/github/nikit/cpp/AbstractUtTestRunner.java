package com.github.nikit.cpp;

/**
 * Created by nik on 27.05.17.
 */

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        classes = {Launcher.class, SwaggerConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = MockMvcPrint.LOG_DEBUG)
@AutoConfigureRestDocs(outputDir = TestConstants.RESTDOCS_SNIPPETS_DIR)
@Transactional
public abstract class AbstractUtTestRunner {

    @Autowired
    protected MockMvc mockMvc;

    @Value("${custom.it.user}")
    protected String username;

    @Value("${custom.it.password}")
    protected String password;

}
