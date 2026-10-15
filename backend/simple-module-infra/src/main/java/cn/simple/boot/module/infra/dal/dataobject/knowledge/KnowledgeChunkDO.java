package cn.simple.boot.module.infra.dal.dataobject.knowledge;

import cn.simple.boot.framework.mybatis.core.dataobject.BaseDO;
import cn.simple.boot.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 业务知识库分块表：每个分块文本与其向量（JSON 数组字符串）。
 *
 * @author Simple Boot
 */
@TableName("infra_knowledge_chunk")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class KnowledgeChunkDO extends BaseDO {

    @TableId
    private Long id;

    /** 来源文档名，便于回溯 */
    private String docName;

    /** 分块正文 */
    private String content;

    /** 该分块的向量，存为 JSON 数组字符串，如 [0.12, -0.03, ...] */
    private String embedding;

}
