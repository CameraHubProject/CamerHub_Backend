package vn.camerahub.gateway.controller;

import jakarta.servlet.http.*;

import java.io.IOException;

import org.springframework.web.bind.annotation.*;
import vn.camerahub.gateway.service.GatewayService;

@RestController
@lombok.RequiredArgsConstructor
public class GatewayController {
    private final GatewayService service;

    @GetMapping("/api/health")
    public Object health() {
        return service.health();
    }

    @RequestMapping("/api/**")
    public void forward(HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.forward(request, response);
    }
}
