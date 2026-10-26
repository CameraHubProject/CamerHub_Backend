package vn.camerahub.micro.exception;

import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ExampleController()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @RestController
    static class ExampleController {
        public record Input(@NotBlank String fullName) {
        }

        @PostMapping("/test")
        public Object input(@Valid @RequestBody Input input) {
            return input;
        }

        @GetMapping("/missing")
        public Object missing() {
            throw new ResourceNotFoundException("Không tìm thấy thiết bị.");
        }

        @GetMapping("/error")
        public Object error() {
            throw new IllegalStateException("private-database-password");
        }

        @GetMapping("/domain")
        public Object domain() {
            throw new RemoteServiceException(org.springframework.http.HttpStatus.CONFLICT, "MARKETPLACE_OUT_OF_STOCK", "Thiết bị vừa hết hàng.", java.util.Map.of("productIds", "Thiết bị vừa hết hàng."));
        }
    }

    @Test
    void validationContainsBackendFieldErrors() throws Exception {
        mvc.perform(post("/test").contentType("application/json").content("{\"fullName\":\"\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400)).andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED")).andExpect(jsonPath("$.errors.fullName").isNotEmpty());
    }

    @Test
    void missingResourcesPreserveBackendHttpStatus() throws Exception {
        mvc.perform(get("/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Không tìm thấy thiết bị.")).andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void unexpectedFailuresDoNotExposeSensitiveDetails() throws Exception {
        mvc.perform(get("/error")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(500)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-database-password"))));
    }

    @Test
    void domainErrorsRetainMachineCodeAndBackendText() throws Exception {
        mvc.perform(get("/domain")).andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("MARKETPLACE_OUT_OF_STOCK")).andExpect(jsonPath("$.message").value("Thiết bị vừa hết hàng.")).andExpect(jsonPath("$.errors.productIds").value("Thiết bị vừa hết hàng."));
    }
}
