package cn.simple.boot.module.infra.controller.admin.knowledge;

import cn.simple.boot.framework.common.pojo.CommonResult;
import cn.simple.boot.module.infra.service.knowledge.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.simple.boot.framework.common.pojo.CommonResult.success;

/**
 * 轻量业务知识库：入库与检索。供页面助手 PageAgent 的 search_knowledge 工具调用。
 */
@Tag(name = "管理后台 - 业务知识库")
@RestController
@RequestMapping("/infra/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @PostMapping("/add")
    @Operation(summary = "新增一篇知识库文档（自动切块向量化）")
    public CommonResult<Integer> add(@RequestBody Map<String, String> body) {
        String docName = body.getOrDefault("docName", "未命名文档");
        String content = body.getOrDefault("content", "");
        return success(knowledgeService.addDocument(docName, content));
    }

    @PostMapping("/search")
    @Operation(summary = "语义检索知识库，返回最相关的分块")
    public CommonResult<List<Map<String, Object>>> search(@RequestBody Map<String, Object> body) {
        String query = (String) body.getOrDefault("query", "");
        int topK = body.get("topK") == null ? 5 : Integer.parseInt(String.valueOf(body.get("topK")));
        return success(knowledgeService.search(query, topK));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的知识库文档")
    public CommonResult<List<Map<String, Object>>> mine() {
        return success(knowledgeService.listMine());
    }

    @GetMapping("/others")
    @Operation(summary = "查询其他人的知识库文档")
    public CommonResult<List<Map<String, Object>>> others(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(knowledgeService.listOthers(keyword));
    }

    @PutMapping("/update")
    @Operation(summary = "修改自己的知识库文档")
    public CommonResult<Integer> update(@RequestBody Map<String, String> body) {
        return success(knowledgeService.updateMine(body.get("docName"), body.get("newDocName"), body.get("content")));
    }

    @DeleteMapping("/doc")
    @Operation(summary = "删除自己的一篇知识库文档")
    public CommonResult<Boolean> deleteDoc(@RequestParam("docName") String docName) {
        knowledgeService.deleteMine(docName);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除一个知识库分块")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        knowledgeService.deleteById(id);
        return success(true);
    }

}
