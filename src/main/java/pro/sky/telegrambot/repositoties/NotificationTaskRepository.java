package pro.sky.telegrambot.repositoties;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.entities.NotificationTask;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {
    // Поиск напоминаний по дате и времени.
    List<NotificationTask> findByDataTimeMessage(LocalDateTime dateTime);

    List<NotificationTask> findByDataTimeMessageBefore(LocalDateTime dateTime);
}
