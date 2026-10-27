package vn.camerahub.shared.dto;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiResponseAdviceTest {
    @RestController
    static class ExampleController {
        @PostMapping("/api/example")
        @SuccessMessage("Đã lưu thông tin từ backend.")
        public Object save() {
            return Map.of("id", "created");
        }

        @PostMapping("/api/delete")
        public ApiResponse<Void> delete() {
            return ApiResponse.success(200, "Đã xóa từ backend.", null);
        }
    }

    @Test
    void mutationNotificationsUseBackendAnnotationAndKeepData() throws Exception {
        MockMvcBuilders.standaloneSetup(new ExampleController()).setControllerAdvice(new ApiResponseAdvice()).build()
                .perform(post("/api/example")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Đã lưu thông tin từ backend.")).andExpect(jsonPath("$.data.id").value("created"));
    }

    @Test
    void explicitResponsesWithNullDataAreNotWrappedTwice() throws Exception {
        MockMvcBuilders.standaloneSetup(new ExampleController()).setControllerAdvice(new ApiResponseAdvice()).build()
                .perform(post("/api/delete")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Đã xóa từ backend.")).andExpect(jsonPath("$.data").doesNotExist());
    }
}
