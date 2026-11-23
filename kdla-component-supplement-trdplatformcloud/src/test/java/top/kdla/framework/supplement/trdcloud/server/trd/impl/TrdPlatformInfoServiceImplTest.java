package top.kdla.framework.supplement.trdcloud.server.trd.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;
import top.kdla.framework.supplement.trdcloud.entity.TrdPlatformInfoEntity;
import top.kdla.framework.supplement.trdcloud.repository.TrdPlatformInfoRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrdPlatformInfoServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        // 离线初始化 MP 实体元数据，使 LambdaQueryWrapper 无需 DB 即可解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TrdPlatformInfoEntity.class);
    }

    @Test
    void getByPCodesDuplicateCodeKeepsFirst() {
        TrdPlatformInfoRepository repository = mock(TrdPlatformInfoRepository.class);
        TrdPlatformInfoEntity first = new TrdPlatformInfoEntity();
        first.setId(1L);
        first.setPlatformCode("P1");
        TrdPlatformInfoEntity second = new TrdPlatformInfoEntity();
        second.setId(2L);
        second.setPlatformCode("P1");
        when(repository.list(ArgumentMatchers.<Wrapper<TrdPlatformInfoEntity>>any())).thenReturn(List.of(first, second));

        TrdPlatformInfoServiceImpl service = new TrdPlatformInfoServiceImpl();
        ReflectionTestUtils.setField(service, "trdPlatformInfoRepository", repository);

        Map<String, TrdPlatformInfoEntity> result = service.getByPCodes(List.of("P1"));

        assertThat(result).hasSize(1);
        assertThat(result.get("P1")).isSameAs(first);
    }

}