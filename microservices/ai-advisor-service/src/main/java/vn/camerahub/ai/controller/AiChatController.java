package vn.camerahub.ai.controller;

import vn.camerahub.ai.service.AiSettings;
import vn.camerahub.ai.service.ConversationService;
import vn.camerahub.ai.dto.AiChatDtos.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.security.Principal;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final ConversationService conversations;
    private final AiSettings settings;
    private final boolean secure;

    public AiChatController(ConversationService conversations, AiSettings settings, @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.conversations = conversations;
        this.settings = settings;
        this.secure = secure;
    }

    @GetMapping("/status")
    public Object status() {
        return Map.of("enabled", settings.enabled(), "webSearchEnabled", settings.enabled() && settings.web());
    }

    @GetMapping("/conversation")
    public Object history(HttpServletRequest request, HttpServletResponse response, Principal user) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("messages", conversations.history(id(request, response), owner(user)));
    }

    @PostMapping("/chat")
    @vn.camerahub.micro.dto.SuccessMessage("AI đã trả lời câu hỏi.")
    public Object chat(@Valid @RequestBody Chat input, HttpServletRequest request, HttpServletResponse response, Principal user) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("messages", conversations.chat(id(request, response), owner(user), request.getRemoteAddr(), input.message().strip(), input.webSearch(), input.productId()));
    }

    @PostMapping("/conversation/reset")
    @vn.camerahub.micro.dto.SuccessMessage("Đã bắt đầu cuộc trò chuyện mới.")
    public Object reset(HttpServletRequest request, HttpServletResponse response, Principal user) {
        conversations.reset(id(request, response), owner(user));
        return Map.of("messages", List.of());
    }

    private String owner(Principal user) {
        return user == null ? "anonymous" : user.getName();
    }

    private UUID id(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) for (var cookie : request.getCookies())
            if ("CAMERAHUB_AI".equals(cookie.getName())) try {
                return UUID.fromString(cookie.getValue());
            } catch (IllegalArgumentException ignored) {
            }
        UUID id = UUID.randomUUID();
        response.addHeader("Set-Cookie", ResponseCookie.from("CAMERAHUB_AI", id.toString()).path("/").httpOnly(true).sameSite("Lax").secure(secure).maxAge(java.time.Duration.ofDays(7)).build().toString());
        return id;
    }

}

