package cn.simple.boot.module.infra.service.knowledge;

import cn.simple.boot.framework.ai.core.OpenAiCompatibleClient;
import cn.simple.boot.framework.security.core.util.SecurityFrameworkUtils;
import cn.simple.boot.module.infra.dal.dataobject.knowledge.KnowledgeChunkDO;
import cn.simple.boot.module.infra.dal.mysql.knowledge.KnowledgeChunkMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.simple.boot.module.infra.enums.ErrorCodeConstants.KNOWLEDGE_NOT_EXISTS;
import static cn.simple.boot.module.infra.enums.ErrorCodeConstants.KNOWLEDGE_NOT_LOGIN;
import static cn.simple.boot.module.infra.enums.ErrorCodeConstants.KNOWLEDGE_NOT_OWNER;

/**
 * 轻量知识库实现：切块 → embed 存库；检索时全量算余弦相似度。
 */
@Service
@Slf4j
@Validated
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 每块约 400 个字符 */
    private static final int CHUNK_SIZE = 400;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;
    @Resource
    private OpenAiCompatibleClient aiClient;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public int addDocument(String docName, String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        List<String> chunks = split(content);
        int inserted = 0;
        for (String chunk : chunks) {
            KnowledgeChunkDO doo = new KnowledgeChunkDO();
            doo.setDocName(docName);
            doo.setContent(chunk);
            float[] vector = aiClient.embed(chunk);
            if (vector != null) {
                try {
                    doo.setEmbedding(objectMapper.writeValueAsString(vector));
                } catch (Exception e) {
                    log.warn("[knowledge] serialize embedding failed: {}", e.getMessage());
                }
            } else {
                log.warn("[knowledge] embed unavailable, store text only (keyword fallback)");
            }
            knowledgeChunkMapper.insert(doo);
            inserted++;
        }
        return inserted;
    }

    @Override
    public List<Map<String, Object>> search(String query, int topK) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        List<KnowledgeChunkDO> all = knowledgeChunkMapper.selectList();
        int limit = topK <= 0 ? 5 : topK;
        float[] queryVector = aiClient.embed(query);
        if (queryVector != null) {
            List<Map<String, Object>> scored = new ArrayList<>();
            for (KnowledgeChunkDO chunk : all) {
                float[] vector = parseVector(chunk.getEmbedding());
                if (vector == null || vector.length != queryVector.length) {
                    continue;
                }
                double score = cosine(queryVector, vector);
                Map<String, Object> item = new HashMap<>();
                item.put("content", chunk.getContent());
                item.put("docName", chunk.getDocName());
                item.put("score", score);
                scored.add(item);
            }
            if (!scored.isEmpty()) {
                scored.sort(Comparator.comparingDouble(m -> (double) m.get("score")));
                java.util.Collections.reverse(scored);
                return scored.subList(0, Math.min(limit, scored.size()));
            }
        }
        log.warn("[knowledge] embedding unavailable or empty, fallback to text match");
        return textSearch(all, query, limit);
    }

    /** 按标题、正文和问句里的词匹配，不依赖固定词表 */
    private List<Map<String, Object>> textSearch(List<KnowledgeChunkDO> all, String query, int limit) {
        String raw = query.trim();
        String focused = stripQuestionWords(raw);
        List<Map<String, Object>> scored = new ArrayList<>();
        for (KnowledgeChunkDO chunk : all) {
            String content = chunk.getContent() == null ? "" : chunk.getContent();
            String docName = chunk.getDocName() == null ? "" : chunk.getDocName();
            String haystack = docName + content;
            int score = 0;
            if (!focused.isEmpty() && haystack.contains(focused)) {
                score += 8;
            }
            if (!raw.isEmpty() && (docName.contains(raw) || content.contains(raw))) {
                score += 8;
            }
            String grams = focused.isEmpty() ? raw : focused;
            for (int i = 0; i + 2 <= grams.length(); i++) {
                String gram = grams.substring(i, i + 2);
                if (haystack.contains(gram)) {
                    score++;
                }
            }
            if (score <= 0) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("content", content);
            item.put("docName", docName);
            item.put("score", (double) score);
            scored.add(item);
        }
        scored.sort(Comparator.comparingDouble(m -> (double) m.get("score")));
        java.util.Collections.reverse(scored);
        return scored.subList(0, Math.min(limit, scored.size()));
    }

    private String stripQuestionWords(String query) {
        String text = query;
        for (String word : new String[]{"是什么", "为什么", "怎么", "如何", "什么", "吗", "呢", "的", "？", "?"}) {
            text = text.replace(word, "");
        }
        return text.trim();
    }

    @Override
    public List<Map<String, Object>> listMine() {
        return groupDocs(selectByCreator(requireUser(), true), null);
    }

    @Override
    public List<Map<String, Object>> listOthers(String keyword) {
        return groupDocs(selectByCreator(requireUser(), false), keyword);
    }

    @Override
    public int updateMine(String docName, String newDocName, String content) {
        String uid = requireUser();
        if (docName == null || docName.isBlank() || content == null || content.isBlank()) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
        List<KnowledgeChunkDO> mine = knowledgeChunkMapper.selectList(ownerDoc(uid, docName));
        if (mine.isEmpty()) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
        knowledgeChunkMapper.delete(ownerDoc(uid, docName));
        String name = newDocName == null || newDocName.isBlank() ? docName.trim() : newDocName.trim();
        return addDocument(name, content);
    }

    @Override
    public void deleteMine(String docName) {
        String uid = requireUser();
        if (docName == null || docName.isBlank()) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
        if (knowledgeChunkMapper.delete(ownerDoc(uid, docName)) == 0) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
    }

    @Override
    public void deleteById(Long id) {
        KnowledgeChunkDO chunk = knowledgeChunkMapper.selectById(id);
        if (chunk == null) {
            throw exception(KNOWLEDGE_NOT_EXISTS);
        }
        if (!requireUser().equals(chunk.getCreator())) {
            throw exception(KNOWLEDGE_NOT_OWNER);
        }
        knowledgeChunkMapper.deleteById(id);
    }

    private String requireUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(KNOWLEDGE_NOT_LOGIN);
        }
        return String.valueOf(userId);
    }

    private QueryWrapper<KnowledgeChunkDO> ownerDoc(String userId, String docName) {
        return new QueryWrapper<KnowledgeChunkDO>().eq("creator", userId).eq("doc_name", docName.trim());
    }

    private List<KnowledgeChunkDO> selectByCreator(String userId, boolean mine) {
        QueryWrapper<KnowledgeChunkDO> wrapper = new QueryWrapper<KnowledgeChunkDO>().orderByAsc("id");
        if (mine) {
            wrapper.eq("creator", userId);
        } else {
            wrapper.ne("creator", userId);
        }
        return knowledgeChunkMapper.selectList(wrapper);
    }

    private List<Map<String, Object>> groupDocs(List<KnowledgeChunkDO> chunks, String keyword) {
        Map<String, List<KnowledgeChunkDO>> grouped = new LinkedHashMap<>();
        for (KnowledgeChunkDO chunk : chunks) {
            String key = String.valueOf(chunk.getCreator()) + "\0" + String.valueOf(chunk.getDocName());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(chunk);
        }
        String kw = keyword == null ? "" : keyword.trim();
        List<Map<String, Object>> docs = new ArrayList<>();
        for (List<KnowledgeChunkDO> parts : grouped.values()) {
            parts.sort(Comparator.comparing(KnowledgeChunkDO::getId, Comparator.nullsLast(Long::compareTo)));
            StringBuilder content = new StringBuilder();
            LocalDateTime updated = null;
            for (KnowledgeChunkDO part : parts) {
                if (part.getContent() != null) {
                    content.append(part.getContent());
                }
                if (part.getUpdateTime() != null && (updated == null || part.getUpdateTime().isAfter(updated))) {
                    updated = part.getUpdateTime();
                }
            }
            String name = parts.get(0).getDocName() == null ? "" : parts.get(0).getDocName();
            String text = content.toString();
            if (!kw.isEmpty() && !name.contains(kw) && !text.contains(kw)) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("docName", name);
            item.put("creator", parts.get(0).getCreator());
            item.put("content", text);
            item.put("chunkCount", parts.size());
            item.put("updateTime", updated == null ? "" : updated.toString());
            docs.add(item);
        }
        docs.sort((a, b) -> String.valueOf(b.get("updateTime")).compareTo(String.valueOf(a.get("updateTime"))));
        return docs;
    }

    /** 简单按字符长度切块 */
    private List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        String text = content.trim();
        int len = text.length();
        for (int i = 0; i < len; i += CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(i + CHUNK_SIZE, len)));
        }
        return chunks;
    }

    private float[] parseVector(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return null;
            }
            float[] vector = new float[node.size()];
            for (int i = 0; i < node.size(); i++) {
                vector[i] = (float) node.path(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            return null;
        }
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

}
