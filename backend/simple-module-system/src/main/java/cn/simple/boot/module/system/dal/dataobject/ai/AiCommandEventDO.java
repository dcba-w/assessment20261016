package cn.simple.boot.module.system.dal.dataobject.ai;

import cn.simple.boot.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户指挥 AI 干活的事件。steps 按执行顺序保存，下次可直接重放。
 */
@TableName("system_ai_command_event")
@KeySequence("system_ai_command_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiCommandEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 用户指令
     */
    private String task;

    /**
     * 发生页面
     */
    private String pagePath;

    /**
     * 处理步骤 JSON，数组顺序即执行顺序
     */
    private String steps;

    /**
     * 步骤数量
     */
    private Integer stepCount;

}
