package io.majide.repository;

import io.majide.domain.TaskRun;
import io.majide.domain.TaskRunStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface TaskRunRepository extends JpaRepository<TaskRun, Long> {

    Optional<TaskRun> findByInstanceIdAndTaskId(Long instanceId, Long taskId);

    List<TaskRun> findByInstanceId(Long instanceId);

    List<TaskRun> findByInstanceIdAndStatus(Long instanceId, TaskRunStatus status);

    // 상태 전이(낙관적 제어)
    @Modifying
    @Query("""
        update TaskRun tr
           set tr.status = :to
         where tr.id = :id and tr.status = :expected
        """)
    int updateStatusIf(Long id, TaskRunStatus expected, TaskRunStatus to);

    // 재시도 스케줄링: attempt +1, 다음 실행 시각 예약
    @Modifying
    @Query("""
        update TaskRun tr
           set tr.status = io.majide.entity.TaskRunStatus.RETRY_WAIT,
               tr.attempt = tr.attempt + 1,
               tr.scheduledAt = :next,
               tr.leaseOwner = null,
               tr.leaseExpireAt = null,
               tr.endedAt = :now
         where tr.id = :id
        """)
    int scheduleRetry(Long id, Instant next, Instant now);

    // 성공/실패 마킹
    @Modifying
    @Query("""
        update TaskRun tr
           set tr.status = :to,
               tr.leaseOwner = null,
               tr.leaseExpireAt = null,
               tr.endedAt = :now
         where tr.id = :id
        """)
    int complete(Long id, TaskRunStatus to, Instant now);

    // 하트비트/임대 연장 (워커가 주기적으로 호출)
    @Modifying
    @Query("""
        update TaskRun tr
           set tr.heartbeatAt = :hb,
               tr.leaseExpireAt = :leaseExpireAt
         where tr.id = :id and tr.status = io.majide.entity.TaskRunStatus.RUNNING
        """)
    int heartbeat(Long id, Instant hb, Instant leaseExpireAt);

    // 명시적 임대 해제(비정상 종료 복구 등)
    @Modifying
    @Query("""
        update TaskRun tr
           set tr.leaseOwner = null,
               tr.leaseExpireAt = null
         where tr.id = :id
        """)
    int releaseLease(Long id);
}
