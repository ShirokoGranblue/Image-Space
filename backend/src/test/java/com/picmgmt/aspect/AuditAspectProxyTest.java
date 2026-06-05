package com.picmgmt.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.annotation.Audit;
import com.picmgmt.common.Result;
import com.picmgmt.entity.AuditLog;
import com.picmgmt.mapper.AuditLogMapper;
import com.picmgmt.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.web.bind.annotation.PathVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class AuditAspectProxyTest {

    @Test
    void annotatedControllerMethod_shouldBeInterceptedThroughSpringAopProxy() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        AuditAspect aspect = new AuditAspect(auditLogMapper, new ObjectMapper(), mock(UserMapper.class));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DummyController());
        proxyFactory.addAspect(aspect);
        DummyApi proxy = proxyFactory.getProxy();

        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(false);
            assertEquals("ok", proxy.delete("image-uuid").getData());
        }

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("IMAGE_DELETE", log.getAction());
        assertEquals("image-uuid", log.getTargetId());
        assertEquals("SUCCESS", log.getResult());
    }

    interface DummyApi {
        Result<String> delete(String uuid);
    }

    static class DummyController implements DummyApi {
        @Override
        @Audit(action = "IMAGE_DELETE", module = "IMAGE", targetType = "image")
        public Result<String> delete(@PathVariable("uuid") String uuid) {
            return Result.ok("ok");
        }
    }
}
