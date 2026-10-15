package cn.simple.boot.module.infra.service.knowledge;

import java.util.List;
import java.util.Map;

/**
 * 轻量业务知识库：文本切块入库 + 向量检索。
 * 不依赖外部向量库，全量在内存做余弦相似度排序，适合内部小体量知识库。
 */
public interface KnowledgeService {

    /**
     * 把一篇文档切块、向量化后入库。
     *
     * @return 新增的分块数
     */
    int addDocument(String docName, String content);

    /**
     * 语义检索最相关的前 topK 个分块。
     *
     * @return 每项含 content / docName / score
     */
    List<Map<String, Object>> search(String query, int topK);

    /** 当前用户自己的文档，按文档名合并分块 */
    List<Map<String, Object>> listMine();

    /** 其他人的文档，可按标题或正文筛选 */
    List<Map<String, Object>> listOthers(String keyword);

    /**
     * 覆盖自己的一篇文档。newDocName 为空时保持原名。
     *
     * @return 重新入库的分块数
     */
    int updateMine(String docName, String newDocName, String content);

    /** 删除自己的一篇文档 */
    void deleteMine(String docName);

    /** 按 id 删除一个分块，仅文档所有者可删 */
    void deleteById(Long id);

}
