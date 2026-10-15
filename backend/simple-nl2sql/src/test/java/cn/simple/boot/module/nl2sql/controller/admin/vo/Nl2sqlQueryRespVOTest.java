package cn.simple.boot.module.nl2sql.controller.admin.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Nl2sqlQueryRespVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonDoesNotExposeSqlRelatedFields() throws Exception {
        Nl2sqlQueryRespVO vo = Nl2sqlQueryRespVO.builder()
                .answer("一共查到 1 条记录。")
                .columns(List.of("cnt"))
                .columnLabels(List.of("数量"))
                .rows(List.of(Map.of("cnt", 1)))
                .build();

        String json = objectMapper.writeValueAsString(vo);

        assertTrue(json.contains("\"answer\""));
        assertTrue(json.contains("\"columns\""));
        assertTrue(json.contains("\"columnLabels\""));
        assertTrue(json.contains("\"rows\""));
        assertFalse(json.contains("\"sql\""));
        assertFalse(json.contains("\"explanation\""));
        assertFalse(json.contains("\"repaired\""));
    }

}
