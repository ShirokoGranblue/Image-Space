package com.picmgmt.audit;

import com.picmgmt.auth.SaTokenPermissionImpl;
import com.picmgmt.cache.CacheService;
import com.picmgmt.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditStreamTicketServiceTest {

    private final CacheService cacheService = mock(CacheService.class);
    private final SaTokenPermissionImpl permissionService = mock(SaTokenPermissionImpl.class);
    private final AuditStreamTicketService service =
            new AuditStreamTicketService(cacheService, permissionService);

    @Test
    void issuedTicketUsesAnExactShortRedisTtl() {
        when(cacheService.setIfAbsent(anyString(), eq("7"), eq(Duration.ofSeconds(30))))
                .thenReturn(true);

        String ticket = service.issue(7L);

        verify(cacheService).setIfAbsent(
                "audit:stream:ticket:" + ticket,
                "7",
                Duration.ofSeconds(30)
        );
    }

    @Test
    void ticketIsBoundToAdminAndConsumedOnce() {
        when(cacheService.take(anyString(), eq(String.class))).thenReturn(Optional.of("7"));
        when(permissionService.getRoleList(7L, "login")).thenReturn(List.of("admin"));

        assertEquals(7L, service.consume("ticket-value"));
        verify(cacheService).take("audit:stream:ticket:ticket-value", String.class);
    }

    @Test
    void rejectsMissingOrAlreadyConsumedTicket() {
        when(cacheService.take(anyString(), eq(String.class))).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.consume("expired"));
    }

    @Test
    void rejectsTicketWhoseUserIsNoLongerAdmin() {
        when(cacheService.take(anyString(), eq(String.class))).thenReturn(Optional.of("7"));
        when(permissionService.getRoleList(7L, "login")).thenReturn(List.of("user"));

        assertThrows(BusinessException.class, () -> service.consume("ticket-value"));
    }
}
