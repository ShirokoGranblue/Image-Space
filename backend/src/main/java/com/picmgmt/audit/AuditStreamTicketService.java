package com.picmgmt.audit;

import com.picmgmt.auth.SaTokenPermissionImpl;
import com.picmgmt.cache.CacheService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditStreamTicketService {

    private static final String KEY_PREFIX = "audit:stream:ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);

    private final CacheService cacheService;
    private final SaTokenPermissionImpl permissionService;

    public String issue(Long userId) {
        String ticket;
        do {
            ticket = UUID.randomUUID().toString().replace("-", "");
        } while (!cacheService.setIfAbsent(
                KEY_PREFIX + ticket,
                String.valueOf(userId),
                TICKET_TTL
        ));
        return ticket;
    }

    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "审计流凭证无效或已过期");
        }
        String loginId = cacheService.take(KEY_PREFIX + ticket, String.class)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "审计流凭证无效或已过期"
                ));
        Long userId;
        try {
            userId = Long.valueOf(loginId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "审计流凭证无效或已过期");
        }
        if (!permissionService.getRoleList(userId, "login").contains("admin")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return userId;
    }
}
