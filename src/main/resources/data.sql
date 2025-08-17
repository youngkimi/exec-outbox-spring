-- 최신 버전 1
INSERT INTO TB_BAT_DAG_VERSION (VERSION, EFFECTIVE_AT)
VALUES (1, '2025-08-01T00:00:00Z')
    ON CONFLICT (VERSION) DO NOTHING;

-- 노드 정의
INSERT INTO TB_BAT_TASK_DEF (VERSION, TASK_KEY, HANDLER) VALUES
                                                             (1,'A','handlerA'), (1,'B','handlerB'), (1,'C','handlerC')
    ON CONFLICT DO NOTHING;

-- 의존성: A -> B -> C
INSERT INTO TB_BAT_TASK_DEPENDENCY (VERSION, FROM_KEY, TO_KEY) VALUES
                                                                   (1,'A','B'), (1,'B','C')
    ON CONFLICT DO NOTHING;